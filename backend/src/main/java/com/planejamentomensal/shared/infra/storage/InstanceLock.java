package com.planejamentomensal.shared.infra.storage;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Lock file in {@code data/} that keeps the same database from being opened twice.
 *
 * <p>Implements POR-10. The file records machine, operating system, PID and process start time. A lock
 * whose process is still running refuses the start; a lock whose process no longer exists (or that was
 * written on another machine) is orphaned and is only taken over when the user confirmed it, which the
 * shell signals with {@code force}.</p>
 *
 * @since 1.0
 */
public final class InstanceLock {

    /** Name of the lock file inside {@code data/}. */
    public static final String FILE_NAME = "instance.lock";

    private final Path file;
    private final LockOwner self;
    private boolean held;

    /**
     * Creates a lock for the given data folder and owner.
     *
     * @param dataDir folder that holds the database
     * @param self    identity of the current process
     */
    public InstanceLock(Path dataDir, LockOwner self) {
        this.file = dataDir.resolve(FILE_NAME);
        this.self = self;
    }

    /**
     * Creates a lock for the current process.
     *
     * @param dataDir folder that holds the database
     * @return the lock, not yet acquired
     */
    public static InstanceLock forCurrentProcess(Path dataDir) {
        return new InstanceLock(dataDir, LockOwner.current());
    }

    /**
     * Takes the lock or explains why it cannot be taken.
     *
     * @param force {@code true} when the user confirmed taking over an orphaned lock
     * @throws InstanceLockedException if another running process holds the lock, or if the lock is orphaned
     *                                 and {@code force} is {@code false}
     * @throws UncheckedIOException    if the lock file cannot be read or written
     */
    public void acquire(boolean force) {
        Optional<LockOwner> current = readOwner();
        if (current.isPresent() && !current.get().equals(self)) {
            LockOwner owner = current.get();
            if (owner.isRunningHere()) {
                throw new InstanceLockedException(InstanceLockedException.Reason.HELD_BY_RUNNING_PROCESS, owner);
            }
            if (!force) {
                throw new InstanceLockedException(InstanceLockedException.Reason.ORPHANED, owner);
            }
        }
        write(self);
        held = true;
    }

    /**
     * Removes the lock file if this process holds it.
     */
    public void release() {
        if (!held) {
            return;
        }
        try {
            if (readOwner().filter(self::equals).isPresent()) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot remove " + file, e);
        } finally {
            held = false;
        }
    }

    /**
     * Returns the lock file.
     *
     * @return {@code data/instance.lock}
     */
    public Path file() {
        return file;
    }

    private Optional<LockOwner> readOwner() {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
        return LockOwner.from(properties);
    }

    private void write(LockOwner owner) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                owner.toProperties().store(writer, "Planejamento Mensal instance lock (POR-10)");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    /**
     * Identity of the process that holds the lock.
     *
     * @param host      machine name
     * @param os        operating system name
     * @param pid       process id
     * @param startedAt process start time, used to tell a reused PID apart
     */
    public record LockOwner(String host, String os, long pid, Instant startedAt) {

        /**
         * Describes the current process.
         *
         * @return owner for this JVM
         */
        public static LockOwner current() {
            ProcessHandle process = ProcessHandle.current();
            Instant start = process.info().startInstant().orElse(Instant.EPOCH);
            return new LockOwner(hostName(), System.getProperty("os.name", "unknown"), process.pid(), start);
        }

        /**
         * Tells whether this owner is a process still running on this machine.
         *
         * @return {@code true} if the PID exists here with the same start time
         */
        public boolean isRunningHere() {
            if (!host.equalsIgnoreCase(hostName())) {
                return false;
            }
            return ProcessHandle.of(pid)
                    .filter(ProcessHandle::isAlive)
                    .flatMap(process -> process.info().startInstant())
                    .map(startedAt::equals)
                    .orElse(false);
        }

        Properties toProperties() {
            Properties properties = new Properties();
            properties.setProperty("host", host);
            properties.setProperty("os", os);
            properties.setProperty("pid", Long.toString(pid));
            properties.setProperty("startedAt", startedAt.toString());
            return properties;
        }

        static Optional<LockOwner> from(Properties properties) {
            try {
                return Optional.of(new LockOwner(
                        properties.getProperty("host", ""),
                        properties.getProperty("os", ""),
                        Long.parseLong(properties.getProperty("pid", "-1")),
                        Instant.parse(properties.getProperty("startedAt", Instant.EPOCH.toString()))));
            } catch (RuntimeException e) {
                // An unreadable lock is treated as orphaned: no live process can be proven to own it
                return Optional.of(new LockOwner("", "", -1, Instant.EPOCH));
            }
        }

        private static String hostName() {
            String name = System.getenv("COMPUTERNAME");
            if (name == null || name.isBlank()) {
                name = System.getenv("HOSTNAME");
            }
            if (name == null || name.isBlank()) {
                try {
                    name = java.net.InetAddress.getLocalHost().getHostName();
                } catch (java.net.UnknownHostException e) {
                    name = "unknown";
                }
            }
            return name.toLowerCase(Locale.ROOT);
        }
    }
}

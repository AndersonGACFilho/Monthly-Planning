package com.planejamentomensal.shared.infra.storage;

import org.springframework.boot.ExitCodeGenerator;

/**
 * Thrown at startup when the database is locked by another instance (POR-10).
 *
 * <p>The process exits with {@link #getExitCode()} so the shell can tell the user what happened and, for an
 * orphaned lock, ask whether to take it over.</p>
 *
 * @since 1.0
 */
public class InstanceLockedException extends RuntimeException implements ExitCodeGenerator {

    /** Exit code when another running instance holds the lock. */
    public static final int EXIT_HELD = 10;
    /** Exit code when the lock is orphaned and needs the user's confirmation. */
    public static final int EXIT_ORPHANED = 11;

    /** Why the lock could not be taken. */
    public enum Reason {
        /** A process that is still running on this machine holds the lock. */
        HELD_BY_RUNNING_PROCESS,
        /** The owner no longer runs here; taking over needs confirmation. */
        ORPHANED
    }

    private final Reason reason;
    private final transient InstanceLock.LockOwner owner;

    /**
     * Creates the exception.
     *
     * @param reason why the lock could not be taken
     * @param owner  owner recorded in the lock file
     */
    public InstanceLockedException(Reason reason, InstanceLock.LockOwner owner) {
        super("Database locked (" + reason + ") by " + owner.host() + " / " + owner.os() + " / PID " + owner.pid());
        this.reason = reason;
        this.owner = owner;
    }

    /**
     * Returns why the lock could not be taken.
     *
     * @return the reason
     */
    public Reason reason() {
        return reason;
    }

    /**
     * Returns the owner recorded in the lock file.
     *
     * @return the owner
     */
    public InstanceLock.LockOwner owner() {
        return owner;
    }

    /**
     * Returns the process exit code for this reason.
     *
     * @return {@link #EXIT_HELD} or {@link #EXIT_ORPHANED}
     */
    @Override
    public int getExitCode() {
        return reason == Reason.HELD_BY_RUNNING_PROCESS ? EXIT_HELD : EXIT_ORPHANED;
    }
}

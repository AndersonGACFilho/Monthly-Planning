package com.planejamentomensal.shared.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.infra.storage.InstanceLock.LockOwner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceLockTest {

    @TempDir
    Path data;

    private final LockOwner current = LockOwner.current();

    private InstanceLock lockOwnedBy(LockOwner owner) {
        return new InstanceLock(data, owner);
    }

    @Test
    @DisplayName("POR-10: free database gets a lock file with host, OS and PID")
    void acquiresFreeLock() throws Exception {
        // Given
        InstanceLock lock = lockOwnedBy(current);
        // When
        lock.acquire(false);
        // Then
        assertThat(Files.readString(lock.file()))
                .contains("pid=" + current.pid()).contains("host=").contains("os=");
    }

    @Test
    @DisplayName("POR-10: lock held by a running process refuses the start, even when forced")
    void runningOwnerRefuses() {
        // Given the current (running) process holds the lock
        lockOwnedBy(current).acquire(false);
        InstanceLock other = lockOwnedBy(new LockOwner("other-host", "Linux", 1, Instant.EPOCH));
        // When / Then
        assertThatThrownBy(() -> other.acquire(true))
                .isInstanceOfSatisfying(InstanceLockedException.class, e -> {
                    assertThat(e.reason()).isEqualTo(InstanceLockedException.Reason.HELD_BY_RUNNING_PROCESS);
                    assertThat(e.getExitCode()).isEqualTo(InstanceLockedException.EXIT_HELD);
                });
    }

    @Test
    @DisplayName("POR-10: orphaned lock (dead PID) needs confirmation")
    void orphanedLockNeedsConfirmation() {
        // Given a lock left by a process that no longer exists
        LockOwner dead = new LockOwner(current.host(), current.os(), current.pid(), Instant.EPOCH);
        lockOwnedBy(dead).acquire(false);
        InstanceLock lock = lockOwnedBy(current);
        // When / Then
        assertThatThrownBy(() -> lock.acquire(false))
                .isInstanceOfSatisfying(InstanceLockedException.class, e -> {
                    assertThat(e.reason()).isEqualTo(InstanceLockedException.Reason.ORPHANED);
                    assertThat(e.getExitCode()).isEqualTo(InstanceLockedException.EXIT_ORPHANED);
                });
    }

    @Test
    @DisplayName("POR-10: lock written on another machine is orphaned here")
    void otherMachineLockIsOrphaned() {
        // Given
        lockOwnedBy(new LockOwner("notebook-windows", "Windows 11", current.pid(), current.startedAt())).acquire(false);
        // When / Then
        assertThatThrownBy(() -> lockOwnedBy(current).acquire(false))
                .isInstanceOfSatisfying(InstanceLockedException.class,
                        e -> assertThat(e.reason()).isEqualTo(InstanceLockedException.Reason.ORPHANED));
    }

    @Test
    @DisplayName("POR-10: confirmed orphaned lock is taken over and released on close")
    void forcedTakeOverAndRelease() throws Exception {
        // Given
        lockOwnedBy(new LockOwner(current.host(), current.os(), current.pid(), Instant.EPOCH)).acquire(false);
        InstanceLock lock = lockOwnedBy(current);
        // When
        lock.acquire(true);
        // Then
        assertThat(Files.readString(lock.file())).contains("startedAt=").doesNotContain("1970");
        lock.release();
        assertThat(lock.file()).doesNotExist();
    }

    @Test
    @DisplayName("POR-10: unreadable lock file is treated as orphaned")
    void unreadableLockIsOrphaned() throws Exception {
        // Given
        Files.writeString(data.resolve(InstanceLock.FILE_NAME), "pid=not-a-number");
        // When / Then
        assertThatThrownBy(() -> lockOwnedBy(current).acquire(false))
                .isInstanceOfSatisfying(InstanceLockedException.class,
                        e -> assertThat(e.reason()).isEqualTo(InstanceLockedException.Reason.ORPHANED));
    }
}

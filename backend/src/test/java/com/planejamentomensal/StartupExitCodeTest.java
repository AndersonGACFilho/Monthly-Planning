package com.planejamentomensal;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.infra.storage.InstanceLock.LockOwner;
import com.planejamentomensal.shared.infra.storage.InstanceLockedException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StartupExitCodeTest {

    @Test
    @DisplayName("POR-10: a locked database wrapped by Spring still yields its exit code")
    void findsExitCodeInCauseChain() {
        // Given
        var locked = new InstanceLockedException(InstanceLockedException.Reason.ORPHANED,
                new LockOwner("h", "Linux", 1, Instant.EPOCH));
        var wrapped = new IllegalStateException("context failed", new RuntimeException("bean failed", locked));
        // When / Then
        assertThat(PlanejamentoMensalApplication.exitCodeOf(wrapped)).isEqualTo(InstanceLockedException.EXIT_ORPHANED);
        assertThat(PlanejamentoMensalApplication.exitCodeOf(new IllegalStateException("other"))).isZero();
    }
}

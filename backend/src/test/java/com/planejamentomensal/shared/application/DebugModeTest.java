package com.planejamentomensal.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DebugModeTest {

    private static final class MemoryStore implements DebugModeStore {
        private boolean enabled;

        @Override
        public boolean load() {
            return enabled;
        }

        @Override
        public void save(boolean newEnabled) {
            enabled = newEnabled;
        }
    }

    private final MemoryStore store = new MemoryStore();
    private final List<Boolean> applied = new ArrayList<>();
    private final DebugMode debugMode = new DebugMode(store, applied::add);

    @Test
    @DisplayName("RNF-11: switching the debug mode keeps it and changes the log levels at once")
    void switches() {
        assertThat(debugMode.enabled()).isFalse();
        debugMode.change(true);
        assertThat(debugMode.enabled()).isTrue();
        assertThat(store.load()).isTrue();
        debugMode.change(false);
        assertThat(applied).containsExactly(true, false);
    }

    @Test
    @DisplayName("RNF-11: on start the stored mode is applied to the log levels")
    void appliesStoredMode() {
        store.save(true);
        debugMode.applyStored();
        assertThat(applied).containsExactly(true);
    }
}

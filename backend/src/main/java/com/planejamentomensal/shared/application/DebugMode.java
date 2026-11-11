package com.planejamentomensal.shared.application;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Debug mode to trace errors (RNF-11): while on, the app logs at DEBUG without a restart and the interface shows the
 * details of each error.
 *
 * @since 1.0
 */
@Service
public class DebugMode {

    private final DebugModeStore store;
    private final LogLevels levels;

    /**
     * Creates the service.
     *
     * @param store  where the mode is kept
     * @param levels log levels of the running app
     */
    public DebugMode(DebugModeStore store, LogLevels levels) {
        this.store = store;
        this.levels = levels;
    }

    /**
     * Tells whether the debug mode is on.
     *
     * @return the stored mode
     */
    public boolean enabled() {
        return store.load();
    }

    /**
     * Switches the debug mode and applies it at once.
     *
     * @param enabled new mode
     */
    public void change(boolean enabled) {
        store.save(enabled);
        levels.apply(enabled);
    }

    /**
     * Applies the stored mode when the app is ready, so a debug session survives a restart.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applyStored() {
        levels.apply(store.load());
    }
}

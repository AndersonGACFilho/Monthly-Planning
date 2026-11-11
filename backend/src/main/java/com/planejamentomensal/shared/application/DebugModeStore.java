package com.planejamentomensal.shared.application;

/**
 * Storage port of the debug mode (RNF-11). It lives outside the database, so opening a backup never changes it.
 *
 * @since 1.0
 */
public interface DebugModeStore {

    /**
     * Reads the stored mode.
     *
     * @return {@code true} when the debug mode is on; {@code false} when nothing was stored
     */
    boolean load();

    /**
     * Stores the mode.
     *
     * @param enabled new mode
     */
    void save(boolean enabled);
}

package com.planejamentomensal.shared.application;

/**
 * Port that changes the log levels of the running app (RNF-11).
 *
 * @since 1.0
 */
@FunctionalInterface
public interface LogLevels {

    /**
     * Raises the app logs to DEBUG, or brings them back to the configured levels.
     *
     * @param debug {@code true} for DEBUG
     */
    void apply(boolean debug);
}

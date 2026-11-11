package com.planejamentomensal.shared.api;

/**
 * New debug mode (RNF-11).
 *
 * @param debugMode {@code true} to turn it on
 * @since 1.0
 */
public record DiagnosticsRequest(boolean debugMode) {
}

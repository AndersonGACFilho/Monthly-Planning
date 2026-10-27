package com.planejamentomensal.imports.domain;

/**
 * Life cycle of an import batch (RF-68, RF-72, Q16).
 *
 * @since 1.0
 */
public enum BatchStatus {
    /** Read and classified, nothing written yet (RF-68). */
    PREVIEW,
    /** Written (RF-72). */
    CONFIRMED,
    /** Preview discarded. */
    CANCELLED,
    /** Confirmed and then undone (RF-72). */
    UNDONE
}

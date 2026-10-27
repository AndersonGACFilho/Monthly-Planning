package com.planejamentomensal.recurrence.domain;

/**
 * State of a generated occurrence: income expected or received (RF-24), expense pending or paid (RF-32).
 *
 * @since 1.0
 */
public enum OccurrenceStatus {
    /** Expected income or pending expense. */
    EXPECTED,
    /** Income received or expense paid. */
    DONE
}

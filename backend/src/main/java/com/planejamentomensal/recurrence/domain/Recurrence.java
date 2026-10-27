package com.planejamentomensal.recurrence.domain;

/**
 * How often a rule generates an occurrence (RF-21).
 *
 * @since 1.0
 */
public enum Recurrence {
    /** Every billing period. */
    MONTHLY,
    /** Only in the first period (one-off income or expense, RF-23). */
    ONCE,
    /** Every N periods from the first one. */
    EVERY_N_MONTHS
}

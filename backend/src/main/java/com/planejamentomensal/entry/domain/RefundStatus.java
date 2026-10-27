package com.planejamentomensal.entry.domain;

/**
 * How much of a purchase was refunded (RN-26, RN-27).
 *
 * @since 1.0
 */
public enum RefundStatus {
    /** Not refunded. */
    NONE,
    /** Partly refunded; the remaining balance is shown (RN-27). */
    PARTIAL,
    /** Fully refunded (RN-26). */
    FULL
}

package com.planejamentomensal.entry.domain;

/**
 * What a purchase is on the bill.
 *
 * @since 1.0
 */
public enum EntryKind {
    /** Purchase or installment. */
    PURCHASE,
    /** Standalone credit without an original purchase (RN-28). */
    CREDIT,
    /** Refund linked to an earlier purchase (RN-26). */
    REFUND,
    /** IOF of an international purchase or of the bill (RF-63e, RF-64d). */
    IOF,
    /** Fee charged by the bank, such as the card plan. */
    FEE
}

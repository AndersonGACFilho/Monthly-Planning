package com.planejamentomensal.entry.domain;

/**
 * State of a card bill (RF-36).
 *
 * @since 1.0
 */
public enum BillStatus {
    /** Open: only projected or partial entries (open XLSX). */
    OPEN,
    /** Closed bill imported from PDF, or past its closing date. */
    CLOSED,
    /** Marked as paid. */
    PAID
}

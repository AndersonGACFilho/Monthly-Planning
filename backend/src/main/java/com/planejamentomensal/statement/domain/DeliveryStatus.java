package com.planejamentomensal.statement.domain;

/**
 * Result of a delivery (SOL-03, RF-92).
 *
 * @since 1.0
 */
public enum DeliveryStatus {
    /** Delivered by the app. */
    SENT,
    /** The app prepared it; the user finishes (WhatsApp, clipboard) and may mark it as sent. */
    AWAITING_USER,
    /** Failed; the detail says why. */
    FAILED
}

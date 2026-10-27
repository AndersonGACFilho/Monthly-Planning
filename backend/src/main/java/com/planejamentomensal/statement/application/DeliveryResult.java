package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.DeliveryStatus;

/**
 * Result of one delivery (SOL-03).
 *
 * @param status SENT, AWAITING_USER or FAILED
 * @param detail error message or saved file, or {@code null}
 * @param action link the interface opens to finish (such as {@code https://wa.me/...}), or {@code null}
 * @since 1.0
 */
public record DeliveryResult(DeliveryStatus status, String detail, String action) {

    /**
     * Delivered by the app.
     *
     * @param detail saved file or note, may be {@code null}
     * @return result
     */
    public static DeliveryResult sent(String detail) {
        return new DeliveryResult(DeliveryStatus.SENT, detail, null);
    }

    /**
     * Prepared; the user finishes it.
     *
     * @param detail saved file, may be {@code null}
     * @param action link to open, may be {@code null}
     * @return result
     */
    public static DeliveryResult awaitingUser(String detail, String action) {
        return new DeliveryResult(DeliveryStatus.AWAITING_USER, detail, action);
    }

    /**
     * Failed.
     *
     * @param detail reason
     * @return result
     */
    public static DeliveryResult failed(String detail) {
        return new DeliveryResult(DeliveryStatus.FAILED, detail, null);
    }
}

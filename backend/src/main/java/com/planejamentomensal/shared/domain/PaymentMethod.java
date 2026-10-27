package com.planejamentomensal.shared.domain;

/**
 * How an expense is paid (RF-30, RF-34).
 *
 * @since 1.0
 */
public enum PaymentMethod {
    /** Bank slip. */
    BOLETO,
    /** Account debit. */
    DEBIT,
    /** PIX transfer. */
    PIX,
    /** Cash. */
    CASH,
    /** Credit card; the card bill already holds the amount (RF-33). */
    CARD
}

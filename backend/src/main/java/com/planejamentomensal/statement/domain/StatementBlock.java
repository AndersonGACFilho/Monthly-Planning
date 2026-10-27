package com.planejamentomensal.statement.domain;

/**
 * Blocks of the statement that a template shows, in its chosen order (RF-82, TPL-02).
 *
 * @since 1.0
 */
public enum StatementBlock {
    /** Total due, income and balance. */
    SUMMARY,
    /** Entries grouped by card. */
    CARDS,
    /** Fixed expenses. */
    FIXED_EXPENSES,
    /** Variable expenses. */
    VARIABLE_EXPENSES,
    /** Installments committed in the next months. */
    COMMITTED,
    /** PIX data for the payment. */
    PAYMENT,
    /** Free note. */
    NOTE
}

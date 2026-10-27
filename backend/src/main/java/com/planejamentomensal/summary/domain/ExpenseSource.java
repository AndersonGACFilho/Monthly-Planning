package com.planejamentomensal.summary.domain;

/**
 * Where an expense comes from (RF-11).
 *
 * @since 1.0
 */
public enum ExpenseSource {
    /** Fixed expense not paid by card (RF-30). */
    FIXED,
    /** Variable expense (RF-34). */
    VARIABLE,
    /** Member share of a card bill entry (RF-26, RF-36). */
    CARD
}

package com.planejamentomensal.entry.domain;

/**
 * Kind of purchase by installment notation (glossary: {@code -}, {@code n/N}, {@code *}).
 *
 * @since 1.0
 */
public enum PurchaseType {
    /** Single purchase ({@code -}). */
    SINGLE,
    /** Installment purchase ({@code n/N}). */
    INSTALLMENT,
    /** Monthly subscription ({@code *}), RN-09. */
    SUBSCRIPTION
}

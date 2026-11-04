package com.planejamentomensal.statement.domain;

/**
 * Columns of the statement line tables that one export can leave out (IMP-60); the category column follows
 * {@link StatementOptions#showCategories()}.
 *
 * @since 1.0
 */
public enum StatementColumn {
    /** Date of card entries and variable expenses. */
    DATE,
    /** Installment of card entries ({@code n/N}, {@code -} or {@code *}). */
    INSTALLMENT,
    /** Split percentage of card entries. */
    SHARE
}

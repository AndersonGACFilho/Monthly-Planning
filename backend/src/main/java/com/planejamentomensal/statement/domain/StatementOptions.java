package com.planejamentomensal.statement.domain;

/**
 * Options of one export (RF-83).
 *
 * @param showCategories show the category of each line
 * @param useAlias       show the displayed description (alias) instead of the original one
 * @param includeFuture  include the installments committed in the next periods
 * @since 1.0
 */
public record StatementOptions(boolean showCategories, boolean useAlias, boolean includeFuture) {

    /** Defaults: categories shown, aliases used, future installments included. */
    public static final StatementOptions DEFAULT = new StatementOptions(true, true, true);
}

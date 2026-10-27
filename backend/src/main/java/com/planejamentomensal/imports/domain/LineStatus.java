package com.planejamentomensal.imports.domain;

/**
 * Classification of an imported line (RF-68, RN-13).
 *
 * @since 1.0
 */
public enum LineStatus {
    /** Not in the system yet; installments project the next ones. */
    NEW,
    /** Already confirmed; ignored by default (RF-69). */
    DUPLICATE,
    /** Matches a projected installment, which becomes confirmed (RN-06). */
    MATCHED,
    /** Looks like an existing entry but amount or date differ; the user decides (RF-69). */
    DIVERGENT
}

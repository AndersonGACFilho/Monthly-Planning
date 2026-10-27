package com.planejamentomensal.imports.domain;

/**
 * What to do with a preview line on confirmation (RF-69).
 *
 * @since 1.0
 */
public enum LineDecision {
    /** Import: create a new purchase, or confirm the matched installment. */
    IMPORT,
    /** Do not import. */
    IGNORE,
    /** Divergent line: replace amount and date of the existing entry (RN-14). */
    UPDATE_EXISTING,
    /** Divergent line waiting for the user's decision (RF-69). */
    UNDECIDED;

    /**
     * Default decision for a classification (RF-69): new and matched are imported, duplicates ignored,
     * divergent lines wait for the user.
     *
     * @param status line status
     * @return the default decision
     */
    public static LineDecision defaultFor(LineStatus status) {
        return switch (status) {
            case NEW, MATCHED -> IMPORT;
            case DUPLICATE -> IGNORE;
            case DIVERGENT -> UNDECIDED;
        };
    }
}

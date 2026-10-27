package com.planejamentomensal.recurrence.domain;

/**
 * What a recurrence rule generates (RF-21, RF-30); incomes and fixed expenses share the model (DRY).
 *
 * @since 1.0
 */
public enum RuleKind {
    /** Income of a member. */
    INCOME,
    /** Fixed expense outside the card detail. */
    FIXED_EXPENSE
}

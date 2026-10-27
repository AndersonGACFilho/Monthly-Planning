package com.planejamentomensal.expense.application;

import com.planejamentomensal.expense.domain.VariableExpense;
import com.planejamentomensal.shared.domain.Money;

/**
 * Read model of a variable expense, for other modules (SOL-06).
 *
 * @param id          expense
 * @param description description
 * @param memberId    member
 * @param categoryId  category, or {@code null}
 * @param amount      amount
 * @since 1.0
 */
public record ExpenseView(long id, String description, long memberId, Long categoryId, Money amount) {

    /**
     * Builds the view of an expense.
     *
     * @param e expense
     * @return its view
     */
    static ExpenseView of(VariableExpense e) {
        return new ExpenseView(e.id(), e.description(), e.memberId(), e.categoryId(), e.amount());
    }
}

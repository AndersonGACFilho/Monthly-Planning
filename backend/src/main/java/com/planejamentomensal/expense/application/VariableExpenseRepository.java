package com.planejamentomensal.expense.application;

import com.planejamentomensal.expense.domain.VariableExpense;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of variable expenses (SOL-04).
 *
 * @since 1.0
 */
public interface VariableExpenseRepository {

    /**
     * Finds an expense.
     *
     * @param id id
     * @return the expense, or empty
     */
    Optional<VariableExpense> findById(Long id);

    /**
     * Lists the expenses of a period.
     *
     * @param billingPeriod period
     * @return expenses
     */
    List<VariableExpense> findByBillingPeriod(BillingPeriod billingPeriod);

    /**
     * Tells whether a member has expenses.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    boolean existsByMemberId(long memberId);

    /**
     * Tells whether a category has expenses.
     *
     * @param categoryId category
     * @return {@code true} if referenced
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Stores an expense and writes it at once.
     *
     * @param expense expense
     * @return the stored expense
     */
    VariableExpense saveAndFlush(VariableExpense expense);

    /**
     * Removes an expense.
     *
     * @param expense expense
     */
    void delete(VariableExpense expense);
}

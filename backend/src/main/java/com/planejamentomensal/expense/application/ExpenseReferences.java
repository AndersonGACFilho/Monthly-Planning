package com.planejamentomensal.expense.application;

import com.planejamentomensal.category.application.CategoryReferences;
import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Members and categories with variable expenses cannot be deleted (RF-01).
 *
 * @since 1.0
 */
@Configuration
public class ExpenseReferences {

    private final VariableExpenseRepository expenses;

    /**
     * Creates the checks.
     *
     * @param expenses expense storage
     */
    public ExpenseReferences(VariableExpenseRepository expenses) {
        this.expenses = expenses;
    }

    /**
     * Member check.
     *
     * @return the check
     */
    @Bean
    MemberReferences expenseMemberReferences() {
        return expenses::existsByMemberId;
    }

    /**
     * Category check.
     *
     * @return the check
     */
    @Bean
    CategoryReferences expenseCategoryReferences() {
        return id -> expenses.existsByCategoryId(id);
    }
}

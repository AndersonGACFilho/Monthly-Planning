package com.planejamentomensal.expense.infra;

import com.planejamentomensal.expense.application.VariableExpenseRepository;
import com.planejamentomensal.expense.domain.VariableExpense;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link VariableExpenseRepository}.
 *
 * @since 1.0
 */
public interface JpaVariableExpenseRepository extends JpaRepository<VariableExpense, Long>, VariableExpenseRepository {
}

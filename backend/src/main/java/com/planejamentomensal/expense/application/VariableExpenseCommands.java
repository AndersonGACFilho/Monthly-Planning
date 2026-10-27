package com.planejamentomensal.expense.application;

import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.expense.domain.VariableExpense;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.Versions;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, edits, deletes and lists variable expenses (RF-34, RF-35).
 *
 * @since 1.0
 */
@Service
@Transactional
public class VariableExpenseCommands {

    private final VariableExpenseRepository expenses;
    private final MemberQueries members;

    /**
     * Creates the commands.
     *
     * @param expenses expense storage
     * @param members  member queries
     */
    public VariableExpenseCommands(VariableExpenseRepository expenses, MemberQueries members) {
        this.expenses = expenses;
        this.members = members;
    }

    /**
     * Lists the expenses of a period, newest first, filtered by member and category (RF-35).
     *
     * @param filter filter
     * @return expenses
     */
    @Transactional(readOnly = true)
    public List<VariableExpense> list(ExpenseFilter filter) {
        return expenses.findByBillingPeriod(filter.billingPeriod()).stream()
                .filter(e -> filter.memberId() == null || e.memberId() == filter.memberId())
                .filter(e -> filter.categoryId() == null || Objects.equals(e.categoryId(), filter.categoryId()))
                .sorted(Comparator.comparing(VariableExpense::expenseDate).reversed()
                        .thenComparing(VariableExpense::description, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Sums a list of expenses (RF-35).
     *
     * @param list expenses
     * @return total
     */
    public static Money total(List<VariableExpense> list) {
        return list.stream().map(VariableExpense::amount).reduce(Money.ZERO, Money::plus);
    }

    /**
     * Loads an expense.
     *
     * @param id id
     * @return the expense
     */
    @Transactional(readOnly = true)
    public VariableExpense get(long id) {
        return expenses.findById(id).orElseThrow(() -> new NotFoundException("variable-expense", id));
    }

    /**
     * Creates an expense.
     *
     * @param details data
     * @return the expense
     */
    public VariableExpense create(ExpenseDetails details) {
        checkMember(details.memberId());
        return expenses.saveAndFlush(new VariableExpense(details));
    }

    /**
     * Replaces the data of an expense.
     *
     * @param id              expense
     * @param details         new data
     * @param expectedVersion version the client edited, or {@code null}
     * @return the expense
     */
    public VariableExpense update(long id, ExpenseDetails details, Long expectedVersion) {
        VariableExpense expense = get(id);
        Versions.check(expectedVersion, expense.version());
        checkMember(details.memberId());
        expense.update(details);
        return expenses.saveAndFlush(expense);
    }

    /**
     * Deletes an expense.
     *
     * @param id expense
     */
    public void delete(long id) {
        expenses.delete(get(id));
    }

    private void checkMember(long memberId) {
        if (members.find(memberId).isEmpty()) {
            throw new BusinessRuleException("card-member-unknown", memberId);
        }
    }
}

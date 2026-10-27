package com.planejamentomensal.expense.api;

import com.planejamentomensal.expense.domain.VariableExpense;
import com.planejamentomensal.shared.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a variable expense.
 *
 * @since 1.0
 */
@Relation(itemRelation = "variableExpense", collectionRelation = "variableExpenses")
public class VariableExpenseModel extends RepresentationModel<VariableExpenseModel> {

    /** Id. */
    public final long id;
    /** Date. */
    public final LocalDate expenseDate;
    /** Billing period. */
    public final String billingPeriod;
    /** Description. */
    public final String description;
    /** Member. */
    public final long memberId;
    /** Category, or {@code null}. */
    public final Long categoryId;
    /** Amount. */
    public final BigDecimal amount;
    /** Payment method. */
    public final PaymentMethod paymentMethod;
    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies an expense.
     *
     * @param expense expense
     */
    public VariableExpenseModel(VariableExpense expense) {
        this.id = expense.id();
        this.expenseDate = expense.expenseDate();
        this.billingPeriod = expense.billingPeriod().toString();
        this.description = expense.description();
        this.memberId = expense.memberId();
        this.categoryId = expense.categoryId();
        this.amount = expense.amount().amount();
        this.paymentMethod = expense.paymentMethod();
        this.version = expense.version();
    }
}

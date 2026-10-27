package com.planejamentomensal.expense.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;

/**
 * An expense entered by hand that is neither fixed nor on a card bill (RF-34).
 *
 * @since 1.0
 */
@Entity
@Table(name = "variable_expense")
public class VariableExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(nullable = false)
    private String description;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false)
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected VariableExpense() {
    }

    /**
     * Creates an expense.
     *
     * @param details data
     */
    public VariableExpense(ExpenseDetails details) {
        update(details);
    }

    /**
     * Replaces the data.
     *
     * @param details new data
     * @throws InvalidInputException with code {@code expense-payment-invalid} for card payments, which come from
     *                               the bill and would be counted twice (RF-11)
     */
    public void update(ExpenseDetails details) {
        if (details.paymentMethod() == PaymentMethod.CARD) {
            throw new InvalidInputException("expense-payment-invalid");
        }
        this.expenseDate = details.expenseDate();
        this.billingPeriod = details.billingPeriod() != null ? details.billingPeriod()
                : BillingPeriod.of(details.expenseDate().getYear(), details.expenseDate().getMonthValue());
        this.description = details.description();
        this.memberId = details.memberId();
        this.categoryId = details.categoryId();
        this.amount = details.amount();
        this.paymentMethod = details.paymentMethod();
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the date.
     *
     * @return expense date
     */
    public LocalDate expenseDate() {
        return expenseDate;
    }

    /**
     * Returns the billing period.
     *
     * @return period
     */
    public BillingPeriod billingPeriod() {
        return billingPeriod;
    }

    /**
     * Returns the description.
     *
     * @return description
     */
    public String description() {
        return description;
    }

    /**
     * Returns the member.
     *
     * @return member id
     */
    public long memberId() {
        return memberId;
    }

    /**
     * Returns the category.
     *
     * @return category id, or {@code null}
     */
    public Long categoryId() {
        return categoryId;
    }

    /**
     * Returns the amount.
     *
     * @return amount
     */
    public Money amount() {
        return amount;
    }

    /**
     * Returns the payment method.
     *
     * @return payment method
     */
    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    /**
     * Returns the version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}

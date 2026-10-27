package com.planejamentomensal.expense.api;

import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data of a variable expense (RF-34).
 *
 * @param expenseDate   date
 * @param billingPeriod period it counts in, {@code yyyy-MM}; the month of the date when missing
 * @param description   description
 * @param memberId      member
 * @param categoryId    category, optional
 * @param amount        amount
 * @param paymentMethod payment method other than card
 * @since 1.0
 */
public record VariableExpenseRequest(
        @NotNull LocalDate expenseDate,
        @Pattern(regexp = "^\\d{4}-\\d{2}$") String billingPeriod,
        @NotBlank @Size(max = 120) String description,
        @NotNull Long memberId,
        Long categoryId,
        @NotNull BigDecimal amount,
        @NotNull PaymentMethod paymentMethod) {

    /**
     * Converts to domain data.
     *
     * @return expense details
     */
    public ExpenseDetails toDetails() {
        return new ExpenseDetails(expenseDate, billingPeriod == null || billingPeriod.isBlank() ? null
                : BillingPeriod.parse(billingPeriod), description.strip(), memberId, categoryId, Money.of(amount),
                paymentMethod);
    }
}

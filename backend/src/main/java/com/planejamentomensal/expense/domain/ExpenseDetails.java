package com.planejamentomensal.expense.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import java.time.LocalDate;

/**
 * Data of a variable expense (RF-34).
 *
 * @param expenseDate   date of the expense
 * @param billingPeriod billing period it counts in
 * @param description   description
 * @param memberId      member
 * @param categoryId    category, or {@code null}
 * @param amount        amount
 * @param paymentMethod payment method (not card: card purchases come from the bill)
 * @since 1.0
 */
public record ExpenseDetails(LocalDate expenseDate, BillingPeriod billingPeriod, String description, long memberId,
        Long categoryId, Money amount, PaymentMethod paymentMethod) {
}

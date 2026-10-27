package com.planejamentomensal.expense.application;

import com.planejamentomensal.shared.domain.BillingPeriod;

/**
 * Filter of the variable expense list (RF-35).
 *
 * @param billingPeriod period
 * @param memberId      member, optional
 * @param categoryId    category, optional
 * @since 1.0
 */
public record ExpenseFilter(BillingPeriod billingPeriod, Long memberId, Long categoryId) {
}

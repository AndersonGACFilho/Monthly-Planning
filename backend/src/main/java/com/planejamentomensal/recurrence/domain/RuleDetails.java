package com.planejamentomensal.recurrence.domain;

import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;

/**
 * Editable data of a {@link RecurrenceRule} (RF-21, RF-30).
 *
 * @param description    description
 * @param memberId       member
 * @param categoryId     category (fixed expenses), or {@code null}
 * @param incomeType     kind of income, or {@code null} for expenses
 * @param defaultAmount  default amount
 * @param recurrence     monthly, once or every N months
 * @param intervalMonths N for {@code EVERY_N_MONTHS}, otherwise 1
 * @param day            expected day of the month (income) or due day (expense)
 * @param validFrom      first billing period
 * @param validUntil     last billing period, or {@code null} for open-ended
 * @param paymentMethod  payment method (fixed expenses), or {@code null}
 * @since 1.0
 */
public record RuleDetails(String description, long memberId, Long categoryId, IncomeType incomeType, Money defaultAmount,
        Recurrence recurrence, int intervalMonths, int day, BillingPeriod validFrom, BillingPeriod validUntil,
        PaymentMethod paymentMethod) {
}

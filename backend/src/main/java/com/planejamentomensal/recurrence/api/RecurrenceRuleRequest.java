package com.planejamentomensal.recurrence.api;

import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Data of an income (RF-21, RF-23) or a fixed expense (RF-30); a one-off income is a rule with recurrence
 * {@code ONCE} in its period.
 *
 * @param description    description
 * @param memberId       member
 * @param categoryId     category (fixed expenses), optional
 * @param incomeType     income type (incomes)
 * @param defaultAmount  default amount
 * @param recurrence     monthly, once or every N months
 * @param intervalMonths N for every N months
 * @param day            expected day or due day
 * @param validFrom      first billing period, {@code yyyy-MM}
 * @param validUntil     last billing period, optional
 * @param paymentMethod  payment method (fixed expenses)
 * @since 1.0
 */
public record RecurrenceRuleRequest(
        @NotBlank @Size(max = 120) String description,
        @NotNull Long memberId,
        Long categoryId,
        IncomeType incomeType,
        @NotNull @PositiveOrZero BigDecimal defaultAmount,
        @NotNull Recurrence recurrence,
        @Min(1) @Max(24) Integer intervalMonths,
        @Min(1) @Max(31) int day,
        @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}$") String validFrom,
        @Pattern(regexp = "^\\d{4}-\\d{2}$") String validUntil,
        PaymentMethod paymentMethod) {

    /**
     * Converts to domain data.
     *
     * @return rule details
     */
    public RuleDetails toDetails() {
        return new RuleDetails(description.strip(), memberId, categoryId,
                incomeType == null ? IncomeType.OTHER : incomeType, Money.of(defaultAmount), recurrence,
                intervalMonths == null ? 1 : intervalMonths, day, BillingPeriod.parse(validFrom),
                validUntil == null || validUntil.isBlank() ? null : BillingPeriod.parse(validUntil),
                paymentMethod == null ? PaymentMethod.BOLETO : paymentMethod);
    }
}

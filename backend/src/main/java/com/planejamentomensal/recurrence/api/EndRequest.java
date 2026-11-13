package com.planejamentomensal.recurrence.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * First billing period without an ended income or fixed expense (IMP-63).
 *
 * @param billingPeriod period, {@code yyyy-MM}
 * @since 1.0
 */
public record EndRequest(@NotNull @Pattern(regexp = "^\\d{4}-\\d{2}$") String billingPeriod) {
}

package com.planejamentomensal.recurrence.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Amount of one period (RF-22, RF-31).
 *
 * @param amount amount
 * @since 1.0
 */
public record AmountRequest(@NotNull @PositiveOrZero BigDecimal amount) {
}

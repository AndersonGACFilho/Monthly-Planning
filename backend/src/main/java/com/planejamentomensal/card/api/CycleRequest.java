package com.planejamentomensal.card.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * New closing and due day from a billing period on (RF-04).
 *
 * @param closingDay closing day
 * @param dueDay     due day
 * @param validFrom  first billing period, {@code yyyy-MM}
 * @since 1.0
 */
public record CycleRequest(@Min(1) @Max(31) int closingDay, @Min(1) @Max(31) int dueDay,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}$") String validFrom) {
}

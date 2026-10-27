package com.planejamentomensal.entry.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A card purchase entered by hand (RF-44, RF-45).
 *
 * @param cardId           card
 * @param purchaseDate     purchase date
 * @param description      description
 * @param amount           amount of each installment; negative for a credit
 * @param installmentCount number of installments, 1 for a single purchase
 * @param memberId         member, optional (card default)
 * @param categoryId       category, optional
 * @param note             note, optional
 * @since 1.0
 */
public record ManualEntryRequest(@NotNull Long cardId, @NotNull LocalDate purchaseDate,
        @NotBlank @Size(max = 120) String description, @NotNull BigDecimal amount,
        @Min(1) @Max(72) int installmentCount, Long memberId, Long categoryId, @Size(max = 500) String note) {
}

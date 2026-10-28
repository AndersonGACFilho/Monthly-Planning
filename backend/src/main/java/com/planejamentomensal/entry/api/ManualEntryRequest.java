package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.ManualPurchase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
 * @param memberId         member, optional (card default; on a replace, the current split)
 * @param categoryId       category, optional
 * @param note             note, optional
 * @param billingPeriod    bill of the first installment, {@code yyyy-MM}, optional (follows the card's cycle)
 * @since 1.0
 */
public record ManualEntryRequest(@NotNull Long cardId, @NotNull LocalDate purchaseDate,
        @NotBlank @Size(max = 120) String description, @NotNull BigDecimal amount,
        @Min(1) @Max(72) int installmentCount, Long memberId, Long categoryId, @Size(max = 500) String note,
        @Pattern(regexp = "^\\d{4}-\\d{2}$") String billingPeriod) {

    /**
     * Converts to the application purchase.
     *
     * @return the purchase
     */
    public ManualPurchase toPurchase() {
        return new ManualPurchase(cardId, purchaseDate, description, Money.of(amount), installmentCount, memberId,
                categoryId, note, billingPeriod == null ? null : BillingPeriod.parse(billingPeriod));
    }
}

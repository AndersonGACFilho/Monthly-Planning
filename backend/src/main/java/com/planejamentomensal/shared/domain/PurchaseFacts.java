package com.planejamentomensal.shared.domain;

import java.time.LocalDate;

/**
 * What identifies one installment for matching across files and imports (RN-10, RN-19).
 *
 * @param cardId            card whose bill holds it (the primary card, Q3)
 * @param purchaseDate      purchase date
 * @param description       description, original or normalized
 * @param installmentNumber installment number, or {@code null} for a single purchase
 * @param installmentCount  installment count, or {@code null}
 * @param amount            installment amount
 * @since 1.0
 */
public record PurchaseFacts(long cardId, LocalDate purchaseDate, String description, Integer installmentNumber,
        Integer installmentCount, Money amount) {
}

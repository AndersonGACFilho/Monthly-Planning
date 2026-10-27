package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;

/**
 * A purchase to record from an imported line or a manual entry.
 *
 * @param cardId            primary card whose bill holds it
 * @param cardLastDigits    final, or {@code null}
 * @param unknownFinal      final not registered (RF-66)
 * @param purchaseDate      purchase date
 * @param description       original description
 * @param installmentNumber installment on this bill, or {@code null}
 * @param installmentCount  installment count, or {@code null}
 * @param amount            amount on this bill
 * @param kind              {@code PURCHASE}, {@code CREDIT}, {@code IOF} or {@code FEE}
 * @param bankCategory      Itaú category suggestion, or {@code null}
 * @param note              note, or {@code null}
 * @param period            billing period of this bill (RN-03)
 * @param entryDate         date on the bill
 * @param occurrence        occurrence of identical lines in the file (RN-12)
 * @param memberId          member the purchase belongs to (RF-02, RF-66)
 * @param categoryId        category, or {@code null}
 * @param reviewed          {@code true} for manual entries, born validated (RF-79c)
 * @since 1.0
 */
public record NewPurchase(long cardId, String cardLastDigits, boolean unknownFinal, LocalDate purchaseDate,
        String description, Integer installmentNumber, Integer installmentCount, Money amount, String kind,
        String bankCategory, String note, BillingPeriod period, LocalDate entryDate, int occurrence, long memberId,
        Long categoryId, boolean reviewed) {
}

package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;

/**
 * Data to create a {@link Purchase}.
 *
 * @param cardId                card whose bill holds the purchase
 * @param cardLastDigits        final, or {@code null}
 * @param purchaseDate          purchase date
 * @param originalDescription   description as on the card
 * @param installmentAmount     amount of each installment
 * @param installmentCount      number of installments, 1 for single purchases
 * @param firstKnownInstallment first installment seen by the system (RN-05)
 * @param type                  single, installment or subscription
 * @param kind                  purchase, credit, refund, IOF or fee
 * @param bankCategory          Itaú category suggestion, or {@code null}
 * @param purchaseKey           key of RN-11
 * @param categoryId            category, or {@code null}
 * @param note                  note, or {@code null}
 * @param reviewStatus          pending (imported) or reviewed (manual, RF-79c)
 * @param unknownFinal          {@code true} if the final is not registered (RF-66)
 * @param splits                member shares
 * @since 1.0
 */
public record PurchaseData(long cardId, String cardLastDigits, LocalDate purchaseDate, String originalDescription,
        Money installmentAmount, int installmentCount, int firstKnownInstallment, PurchaseType type, EntryKind kind,
        String bankCategory, String purchaseKey, Long categoryId, String note, ReviewStatus reviewStatus,
        boolean unknownFinal, List<PurchaseSplit> splits) {
}

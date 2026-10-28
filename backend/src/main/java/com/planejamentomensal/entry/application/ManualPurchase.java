package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;

/**
 * A purchase typed by the user instead of imported (RF-44, RF-45).
 *
 * @param cardId            card (an additional card resolves to its primary card)
 * @param purchaseDate      purchase date
 * @param description       description
 * @param installmentAmount amount of each installment
 * @param installmentCount  number of installments, 1 for a single purchase
 * @param memberId          member, or {@code null} for the card's default member (or, on a replace, the current split)
 * @param categoryId        category, or {@code null}
 * @param note              note, or {@code null}
 * @param billingPeriod     bill of the first installment, or {@code null} to follow the card's cycle
 * @since 1.0
 */
public record ManualPurchase(long cardId, LocalDate purchaseDate, String description, Money installmentAmount,
        int installmentCount, Long memberId, Long categoryId, String note, BillingPeriod billingPeriod) {

    /**
     * Returns the same purchase with another note.
     *
     * @param newNote note, or {@code null}
     * @return the purchase
     */
    public ManualPurchase withNote(String newNote) {
        return new ManualPurchase(cardId, purchaseDate, description, installmentAmount, installmentCount, memberId,
                categoryId, newNote, billingPeriod);
    }
}

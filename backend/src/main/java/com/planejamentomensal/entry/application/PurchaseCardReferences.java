package com.planejamentomensal.entry.application;

import com.planejamentomensal.card.application.CardReferences;
import org.springframework.stereotype.Component;

/**
 * A card with purchases or bills cannot be deleted, only inactivated.
 *
 * @since 1.0
 */
@Component
public class PurchaseCardReferences implements CardReferences {

    private final PurchaseRepository purchases;
    private final CardBillRepository bills;

    /**
     * Creates the check.
     *
     * @param purchases purchase storage
     * @param bills     bill storage
     */
    public PurchaseCardReferences(PurchaseRepository purchases, CardBillRepository bills) {
        this.purchases = purchases;
        this.bills = bills;
    }

    /**
     * Tells whether purchases use the card.
     *
     * @param cardId card id
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long cardId) {
        return purchases.existsByCardId(cardId) || bills.existsByCardId(cardId);
    }
}

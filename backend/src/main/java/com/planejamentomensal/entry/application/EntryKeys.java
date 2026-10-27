package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.EntryKeyFactory;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;

/**
 * Keys of RN-10 and RN-11 for other modules, so every flow builds them the same way (SPEC section 13, DRY).
 *
 * @since 1.0
 */
public final class EntryKeys {

    /**
     * Purchase and installment keys of one line.
     *
     * @param purchaseKey    key of RN-11
     * @param installmentKey key of RN-10
     */
    public record Keys(String purchaseKey, String installmentKey) {
    }

    private EntryKeys() {
    }

    /**
     * Builds both keys.
     *
     * @param cardId            primary card
     * @param date              purchase date
     * @param description       original description
     * @param amount            installment amount
     * @param installmentNumber installment, or {@code null}
     * @param installmentCount  count, or {@code null}
     * @param occurrence        occurrence of identical lines (RN-12)
     * @return the keys
     */
    public static Keys of(long cardId, LocalDate date, String description, Money amount, Integer installmentNumber,
            Integer installmentCount, int occurrence) {
        int count = installmentCount == null ? 1 : installmentCount;
        String purchaseKey = EntryKeyFactory.purchaseKey(cardId, date, description, amount, count, occurrence);
        return new Keys(purchaseKey, EntryKeyFactory.installmentKey(purchaseKey, installmentNumber, null));
    }
}

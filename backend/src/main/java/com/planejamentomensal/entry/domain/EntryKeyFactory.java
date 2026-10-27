package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;

/**
 * Keys of installments and purchases (RN-10 to RN-12).
 *
 * <p>The installment key is card + purchase date + normalized description + total amount + installment
 * number (RN-10); the purchase key drops the installment number (RN-11). Identical legitimate purchases on
 * the same day are told apart by their occurrence in the file (RN-12, RN-25). The keys give exact
 * idempotence (RF-73); matching across formats uses {@link com.planejamentomensal.shared.domain.PurchaseMatcher}
 * (RN-19, Q4).</p>
 *
 * @since 1.0
 */
public final class EntryKeyFactory {

    private EntryKeyFactory() {
    }

    /**
     * Builds the purchase key (RN-11).
     *
     * @param cardId            primary card
     * @param purchaseDate      purchase date
     * @param description       original or normalized description
     * @param installmentAmount amount of one installment
     * @param installmentCount  installment count, 1 for single purchases
     * @param occurrence        occurrence of identical lines in the file, starting at 1 (RN-12)
     * @return the key
     */
    public static String purchaseKey(long cardId, LocalDate purchaseDate, String description, Money installmentAmount,
            int installmentCount, int occurrence) {
        Money total = installmentAmount.times(Math.max(1, installmentCount));
        return "%d|%s|%s|%d|%d".formatted(cardId, purchaseDate, DescriptionNormalizer.normalize(description),
                total.cents(), occurrence);
    }

    /**
     * Builds the installment key (RN-10).
     *
     * @param purchaseKey       key of the purchase
     * @param installmentNumber installment number, or {@code null} for single purchases
     * @param period            for subscriptions, the billing period of the charge; otherwise {@code null}
     * @return the key
     */
    public static String installmentKey(String purchaseKey, Integer installmentNumber, String period) {
        return purchaseKey + "|" + (installmentNumber == null ? "-" : installmentNumber) + (period == null ? "" : "|" + period);
    }
}

package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.PurchaseFacts;

/**
 * An entry already stored, as seen by the import classification (SOL-06: the import module does not see
 * the entry domain).
 *
 * @param entryId        entry
 * @param installmentKey exact key (RN-10)
 * @param state          projected, confirmed or unconfirmed
 * @param facts          card, date, description, installment and amount
 * @param period         billing period of the entry
 * @param subscription   {@code true} for a subscription charge (RN-09)
 * @since 1.0
 */
public record ExistingEntry(long entryId, String installmentKey, State state, PurchaseFacts facts, BillingPeriod period,
        boolean subscription) {

    /** Entry state, mirroring the entry module. */
    public enum State {
        /** Projected ahead (RN-04). */
        PROJECTED,
        /** Seen on a bill. */
        CONFIRMED,
        /** Projected but missing (RN-07). */
        UNCONFIRMED
    }
}

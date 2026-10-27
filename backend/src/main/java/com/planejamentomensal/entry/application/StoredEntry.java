package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.PurchaseFacts;

/**
 * An entry as other modules see it for matching (SOL-06).
 *
 * @param entryId        entry
 * @param purchaseId     purchase
 * @param installmentKey exact key (RN-10)
 * @param status         {@code PROJECTED}, {@code CONFIRMED} or {@code UNCONFIRMED}
 * @param facts          card, date (charge date for subscriptions), description, installment and amount
 * @param period         billing period
 * @param subscription   {@code true} for subscription charges
 * @param importBatchId  batch that created or confirmed it, or {@code null} for manual entries
 * @since 1.0
 */
public record StoredEntry(long entryId, long purchaseId, String installmentKey, String status, PurchaseFacts facts,
        BillingPeriod period, boolean subscription, Long importBatchId) {
}

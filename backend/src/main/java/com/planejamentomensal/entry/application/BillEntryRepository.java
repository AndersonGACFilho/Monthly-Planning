package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of bill entries (SOL-04).
 *
 * @since 1.0
 */
public interface BillEntryRepository {

    /**
     * Finds an entry.
     *
     * @param id id
     * @return the entry, or empty
     */
    Optional<BillEntry> findById(Long id);

    /**
     * Lists the entries on the bills of a card.
     *
     * @param cardId primary card
     * @return entries
     */
    List<BillEntry> findByCardBillCardId(long cardId);

    /**
     * Lists the entries of a billing period, on every card.
     *
     * @param billingPeriod period
     * @return entries
     */
    List<BillEntry> findByCardBillBillingPeriod(BillingPeriod billingPeriod);

    /**
     * Lists the entries of a bill.
     *
     * @param cardBillId bill
     * @return entries
     */
    List<BillEntry> findByCardBillId(long cardBillId);

    /**
     * Lists the installments of a purchase.
     *
     * @param purchaseId purchase
     * @return entries
     */
    List<BillEntry> findByPurchaseIdOrderByEntryDate(long purchaseId);

    /**
     * Lists the entries of a purchase in a state.
     *
     * @param purchaseId purchase
     * @param status     state
     * @return entries
     */
    List<BillEntry> findByPurchaseIdAndStatus(long purchaseId, EntryStatus status);

    /**
     * Stores an entry.
     *
     * @param entry entry
     * @return the stored entry
     */
    BillEntry save(BillEntry entry);

    /**
     * Removes an entry.
     *
     * @param entry entry
     */
    void delete(BillEntry entry);
}

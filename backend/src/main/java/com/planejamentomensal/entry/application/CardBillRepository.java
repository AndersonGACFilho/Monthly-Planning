package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of card bills (SOL-04).
 *
 * @since 1.0
 */
public interface CardBillRepository {

    /**
     * Finds a bill.
     *
     * @param id id
     * @return the bill, or empty
     */
    Optional<CardBill> findById(Long id);

    /**
     * Finds the bill of a card in a period.
     *
     * @param cardId        primary card
     * @param billingPeriod period
     * @return the bill, or empty
     */
    Optional<CardBill> findByCardIdAndBillingPeriod(long cardId, BillingPeriod billingPeriod);

    /**
     * Lists the bills of a card.
     *
     * @param cardId primary card
     * @return bills
     */
    List<CardBill> findByCardId(long cardId);

    /**
     * Lists the bills of a period.
     *
     * @param billingPeriod period
     * @return bills of every card
     */
    List<CardBill> findByBillingPeriod(BillingPeriod billingPeriod);

    /**
     * Tells whether a card has bills.
     *
     * @param cardId card
     * @return {@code true} if it has any bill
     */
    boolean existsByCardId(long cardId);

    /**
     * Stores a bill.
     *
     * @param bill bill
     * @return the stored bill
     */
    CardBill save(CardBill bill);

    /**
     * Removes a bill.
     *
     * @param bill bill
     */
    void delete(CardBill bill);
}

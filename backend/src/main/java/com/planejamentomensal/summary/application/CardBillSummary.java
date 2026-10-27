package com.planejamentomensal.summary.application;

import com.planejamentomensal.entry.application.CardBillView;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CardBillBreakdown;
import java.util.List;

/**
 * Total of a card in a billing period (RF-14, RF-36, RF-37).
 *
 * @param cardId             primary card
 * @param nickname           card nickname
 * @param lastDigits         finals of the card and its additional cards
 * @param bill               bill dates and status
 * @param breakdown          total split into one-off, installments and subscriptions; CONFIRMED and PROJECTED
 *                           entries (Q6)
 * @param committedFuture    projected installments in later periods (RF-14)
 * @param unconfirmedCount   entries left out of the total because they are UNCONFIRMED (Q6)
 * @param pendingReviewCount entries still pending in the review queue (RF-79a)
 * @since 1.0
 */
public record CardBillSummary(long cardId, String nickname, List<String> lastDigits, CardBillView bill,
        CardBillBreakdown breakdown, Money committedFuture, int unconfirmedCount, int pendingReviewCount) {

    /**
     * Returns the bill total.
     *
     * @return total
     */
    public Money total() {
        return breakdown.total();
    }
}

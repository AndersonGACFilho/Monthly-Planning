package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.entry.domain.SplitPolicy;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One line of the card entries tab (SPEC section 7, RF-40).
 *
 * @param entryId             entry
 * @param purchaseId          purchase
 * @param cardBillId          bill
 * @param cardId              primary card
 * @param cardLastDigits      final, or {@code null}
 * @param billingPeriod       period
 * @param entryDate           date on the bill
 * @param purchaseDate        purchase date
 * @param originalDescription description as on the card, never changed
 * @param displayDescription  alias or override, otherwise the original (RF-46 to RF-50)
 * @param renamed             {@code true} when an alias or override is shown
 * @param aliasId             alias of the description, or {@code null}
 * @param installment         {@code n/N}, {@code -} or {@code *}
 * @param type                {@code SINGLE}, {@code INSTALLMENT} or {@code SUBSCRIPTION}
 * @param kind                purchase, credit, refund, IOF or fee
 * @param amount              amount; negative for refunds and credits
 * @param status              projected, confirmed or unconfirmed
 * @param categoryId          category, or {@code null} for "not classified"
 * @param splits              member shares
 * @param reviewStatus        pending or reviewed
 * @param refundStatus        none, partial or full
 * @param note                note, or {@code null}
 * @param manual              {@code true} for a purchase typed by the user that no imported bill has seen yet,
 *                            so it can still be replaced or deleted
 * @param version             purchase version for {@code If-Match}
 * @since 1.0
 */
public record EntryRow(long entryId, long purchaseId, long cardBillId, long cardId, String cardLastDigits,
        BillingPeriod billingPeriod, LocalDate entryDate, LocalDate purchaseDate, String originalDescription,
        String displayDescription, boolean renamed, Long aliasId, String installment, String type, String kind,
        Money amount, String status, Long categoryId, List<ReviewItem.Share> splits, String reviewStatus,
        String refundStatus, String note, boolean manual, long version) {

    /**
     * Divides the amount between the members of the purchase without losing cents (RF-26).
     *
     * @param fallbackMemberId member that takes the whole amount when the purchase has no shares (the card's default)
     * @return amount of each member, in split order
     */
    public Map<Long, Money> memberAmounts(long fallbackMemberId) {
        Map<Long, Money> result = new LinkedHashMap<>();
        if (splits.isEmpty()) {
            result.put(fallbackMemberId, amount);
            return result;
        }
        List<PurchaseSplit> shares = splits.stream()
                .map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount()))
                .toList();
        SplitPolicy.divide(amount, shares).forEach(s -> result.merge(s.memberId(), s.amount(), Money::plus));
        return result;
    }
}

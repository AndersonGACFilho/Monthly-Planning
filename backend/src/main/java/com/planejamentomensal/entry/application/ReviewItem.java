package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * One purchase waiting in the review queue (RF-74, RF-75).
 *
 * @param purchaseId            purchase
 * @param entryId               entry on the latest bill
 * @param cardId                card
 * @param cardLastDigits        final, or {@code null}
 * @param unknownFinal          final not registered (RF-66)
 * @param billingPeriod         period of the entry
 * @param purchaseDate          purchase date
 * @param originalDescription   description as on the card
 * @param installment           {@code n/N}, {@code -} or {@code *}
 * @param amount                amount of the entry
 * @param kind                  purchase, credit, refund, IOF or fee
 * @param bankCategory          Itaú category suggestion, or {@code null}
 * @param categoryId            category, or {@code null}
 * @param splits                member shares
 * @param note                  note, or {@code null}
 * @param refundOfPurchaseId    refunded purchase, or {@code null}
 * @param importBatchId         batch that created it, or {@code null}
 * @param subscriptionSuggested {@code true} to suggest {@code *} (RN-23)
 * @param displayDescription    alias or override, otherwise the original (RF-75)
 * @param aliasId               alias of the description, or {@code null}
 * @param suggestedCategoryId   category of the alias or last used for the description (RF-75, RF-08)
 * @param mergeAliasId          alias of the same store to group with (RN-24), or {@code null}
 * @param mergeAliasName        its name, or {@code null}
 * @param version               version for {@code If-Match}
 * @since 1.0
 */
public record ReviewItem(long purchaseId, long entryId, long cardId, String cardLastDigits, boolean unknownFinal,
        BillingPeriod billingPeriod, LocalDate purchaseDate, String originalDescription, String installment, Money amount,
        String kind, String bankCategory, Long categoryId, List<Share> splits, String note, Long refundOfPurchaseId,
        Long importBatchId, boolean subscriptionSuggested, String displayDescription, Long aliasId,
        Long suggestedCategoryId, Long mergeAliasId, String mergeAliasName, long version) {

    /**
     * Share of one member.
     *
     * @param memberId   member
     * @param percentage percentage, or {@code null}
     * @param amount     fixed amount, or {@code null}
     */
    public record Share(long memberId, BigDecimal percentage, Money amount) {
    }
}

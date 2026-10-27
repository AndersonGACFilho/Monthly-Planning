package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.ReviewItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a pending purchase in the review queue (RF-74, RF-75).
 *
 * @since 1.0
 */
@Relation(itemRelation = "item", collectionRelation = "items")
public class ReviewItemModel extends RepresentationModel<ReviewItemModel> {

    /**
     * Share of one member.
     *
     * @param memberId   member
     * @param percentage percentage, or {@code null}
     * @param amount     fixed amount, or {@code null}
     */
    public record Share(long memberId, BigDecimal percentage, BigDecimal amount) {
    }

    /** Purchase id. */
    public final long purchaseId;
    /** Entry on the latest bill. */
    public final long entryId;
    /** Card. */
    public final long cardId;
    /** Card final. */
    public final String cardLastDigits;
    /** Final not registered (RF-66). */
    public final boolean unknownFinal;
    /** Billing period. */
    public final String billingPeriod;
    /** Purchase date. */
    public final LocalDate purchaseDate;
    /** Description as on the card. */
    public final String originalDescription;
    /** Installment: {@code n/N}, {@code -} or {@code *}. */
    public final String installment;
    /** Amount. */
    public final BigDecimal amount;
    /** Kind. */
    public final String kind;
    /** Itaú category suggestion. */
    public final String bankCategory;
    /** Category. */
    public final Long categoryId;
    /** Member shares. */
    public final List<Share> splits;
    /** Note. */
    public final String note;
    /** Refunded purchase. */
    public final Long refundOfPurchaseId;
    /** Import batch. */
    public final Long importBatchId;
    /** Suggest marking as subscription (RN-23). */
    public final boolean subscriptionSuggested;
    /** Version for {@code If-Match}. */
    public final long version;

    /**
     * Copies an item.
     *
     * @param item item
     */
    public ReviewItemModel(ReviewItem item) {
        this.purchaseId = item.purchaseId();
        this.entryId = item.entryId();
        this.cardId = item.cardId();
        this.cardLastDigits = item.cardLastDigits();
        this.unknownFinal = item.unknownFinal();
        this.billingPeriod = item.billingPeriod().toString();
        this.purchaseDate = item.purchaseDate();
        this.originalDescription = item.originalDescription();
        this.installment = item.installment();
        this.amount = item.amount().amount();
        this.kind = item.kind();
        this.bankCategory = item.bankCategory();
        this.categoryId = item.categoryId();
        this.splits = item.splits().stream()
                .map(s -> new Share(s.memberId(), s.percentage(), s.amount() == null ? null : s.amount().amount()))
                .toList();
        this.note = item.note();
        this.refundOfPurchaseId = item.refundOfPurchaseId();
        this.importBatchId = item.importBatchId();
        this.subscriptionSuggested = item.subscriptionSuggested();
        this.version = item.version();
    }
}

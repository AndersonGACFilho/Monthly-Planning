package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.EntryRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a card entry (SPEC section 7 fields, section 12 example).
 *
 * @since 1.0
 */
@Relation(itemRelation = "billEntry", collectionRelation = "billEntries")
public class BillEntryModel extends RepresentationModel<BillEntryModel> {

    /** Id. */
    public final long id;
    /** Purchase. */
    public final long purchaseId;
    /** Primary card. */
    public final long cardId;
    /** Card final. */
    public final String cardLastDigits;
    /** Billing period. */
    public final String billingPeriod;
    /** Date on the bill. */
    public final LocalDate entryDate;
    /** Purchase date. */
    public final LocalDate purchaseDate;
    /** Description as on the card; read-only. */
    public final String originalDescription;
    /** Alias or override, otherwise the original. */
    public final String displayDescription;
    /** {@code n/N}, {@code -} or {@code *}. */
    public final String installment;
    /** Type. */
    public final String type;
    /** Kind. */
    public final String kind;
    /** Amount; negative for refunds and credits. */
    public final BigDecimal amount;
    /** Status. */
    public final String status;
    /** Category. */
    public final Long categoryId;
    /** Member shares. */
    public final List<ReviewItemModel.Share> splits;
    /** Review state. */
    public final String reviewStatus;
    /** Refund state. */
    public final String refundStatus;
    /** Note. */
    public final String note;
    /** Purchase version for {@code If-Match}. */
    public final long version;

    /**
     * Copies a row.
     *
     * @param row row
     */
    public BillEntryModel(EntryRow row) {
        this.id = row.entryId();
        this.purchaseId = row.purchaseId();
        this.cardId = row.cardId();
        this.cardLastDigits = row.cardLastDigits();
        this.billingPeriod = row.billingPeriod().toString();
        this.entryDate = row.entryDate();
        this.purchaseDate = row.purchaseDate();
        this.originalDescription = row.originalDescription();
        this.displayDescription = row.displayDescription();
        this.installment = row.installment();
        this.type = row.type();
        this.kind = row.kind();
        this.amount = row.amount().amount();
        this.status = row.status();
        this.categoryId = row.categoryId();
        this.splits = row.splits().stream()
                .map(s -> new ReviewItemModel.Share(s.memberId(), s.percentage(), s.amount() == null ? null : s.amount().amount()))
                .toList();
        this.reviewStatus = row.reviewStatus();
        this.refundStatus = row.refundStatus();
        this.note = row.note();
        this.version = row.version();
    }
}

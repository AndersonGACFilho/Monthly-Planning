package com.planejamentomensal.summary.api;

import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.application.CardBillSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a card total in a billing period (RF-14, RF-36, RF-37).
 *
 * @since 1.0
 */
@Relation(itemRelation = "cardBill", collectionRelation = "cardBills")
public class CardBillModel extends RepresentationModel<CardBillModel> {

    /** Primary card. */
    public final long cardId;
    /** Card nickname. */
    public final String nickname;
    /** Finals. */
    public final List<String> lastDigits;
    /** Billing period. */
    public final String billingPeriod;
    /** Closing date, read from a file or forecast. */
    public final LocalDate closingDate;
    /** Due date, read from a file or forecast. */
    public final LocalDate dueDate;
    /** {@code OPEN}, {@code CLOSED} or {@code PAID}. */
    public final String status;
    /** {@code true} while only the open bill was imported. */
    public final boolean partial;
    /** Total printed on the imported file, or {@code null}. */
    public final BigDecimal fileTotal;
    /** Bill total: CONFIRMED and PROJECTED entries. */
    public final BigDecimal total;
    /** One-off purchases, refunds, IOF and fees. */
    public final BigDecimal single;
    /** Installments. */
    public final BigDecimal installments;
    /** Subscriptions. */
    public final BigDecimal subscriptions;
    /** Projected installments in later periods. */
    public final BigDecimal committedFuture;
    /** UNCONFIRMED entries left out of the total. */
    public final int unconfirmedCount;
    /** Entries pending review. */
    public final int pendingReviewCount;
    /** Version of the stored bill for {@code If-Match}. */
    public final long version;

    /**
     * Copies a card total.
     *
     * @param s card total
     */
    public CardBillModel(CardBillSummary s) {
        this.cardId = s.cardId();
        this.nickname = s.nickname();
        this.lastDigits = s.lastDigits();
        this.billingPeriod = s.bill().billingPeriod().toString();
        this.closingDate = s.bill().closingDate();
        this.dueDate = s.bill().dueDate();
        this.status = s.bill().status();
        this.partial = s.bill().partial();
        this.fileTotal = amount(s.bill().fileTotal());
        this.total = s.total().amount();
        this.single = s.breakdown().single().amount();
        this.installments = s.breakdown().installments().amount();
        this.subscriptions = s.breakdown().subscriptions().amount();
        this.committedFuture = s.committedFuture().amount();
        this.unconfirmedCount = s.unconfirmedCount();
        this.pendingReviewCount = s.pendingReviewCount();
        this.version = s.bill().version();
    }

    private static BigDecimal amount(Money money) {
        return money == null ? null : money.amount();
    }
}

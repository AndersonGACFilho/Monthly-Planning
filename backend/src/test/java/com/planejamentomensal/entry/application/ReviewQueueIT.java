package com.planejamentomensal.entry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.category.application.SaveCategoryUseCase;
import com.planejamentomensal.entry.domain.ReviewStatus;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.StaleVersionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ReviewQueueIT {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private SaveCategoryUseCase categories;
    @Autowired private EntryLedger ledger;
    @Autowired private ReviewQueue queue;
    @Autowired private PurchaseRepository purchases;

    private LedgerFixture f;
    private long market;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "2");
        market = categories.create("Mercado", null, "#000000", "cart").id();
    }

    private long pending(String description, String amount) {
        return ledger.addPurchase(1L, f.purchase(description, amount, LocalDate.of(2026, 9, 3), OCT, null, null, "PURCHASE"))
                .purchase().id();
    }

    private static ReviewEdit edit(List<ReviewItem.Share> splits, Long category) {
        return new ReviewEdit(splits, category, false, null, false);
    }

    @Test
    @DisplayName("RF-74, RF-79, RF-79a: imported purchases wait in the queue, filtered and counted")
    void listsPending() {
        pending("PADARIA", "12.00");
        pending("FARMACIA", "30.00");
        assertThat(queue.pending(f.card, OCT, 1L)).hasSize(2);
        assertThat(queue.pending(f.card, OCT.next(), null)).isEmpty();
        ReviewQueue.Summary summary = queue.summary(OCT);
        assertThat(summary.count()).isGreaterThanOrEqualTo(2);
        assertThat(summary.total().compareTo(Money.of("42.00"))).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("RF-76: validating needs a complete split and, for a main member, a category")
    void validationRules() {
        long id = pending("MERCADO DO BAIRRO", "100.00");
        var half = List.of(new ReviewItem.Share(f.main, new BigDecimal("60"), null));
        assertThatThrownBy(() -> queue.apply(List.of(id), edit(half, market), true, null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("review-split-incomplete");
        assertThatThrownBy(() -> queue.apply(List.of(id), ReviewEdit.none(), true, null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("review-category-required");
        var shared = List.of(new ReviewItem.Share(f.main, new BigDecimal("50"), null),
                new ReviewItem.Share(f.thirdParty, new BigDecimal("50"), null));
        ReviewItem item = queue.apply(List.of(id), edit(shared, market), true, null).getFirst();
        assertThat(item.splits()).hasSize(2);
        assertThat(purchases.findById(id).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.REVIEWED);
        assertThat(queue.pending(f.card, null, null)).noneMatch(i -> i.purchaseId() == id);
    }

    @Test
    @DisplayName("RF-76: purchases that belong only to third parties may stay without category")
    void thirdPartyWithoutCategory() {
        long id = pending("PRESENTE TIA", "80.00");
        var aunt = List.of(new ReviewItem.Share(f.thirdParty, null, Money.of("80.00")));
        queue.apply(List.of(id), new ReviewEdit(aunt, null, true, "dela", false), true, null);
        assertThat(purchases.findById(id).orElseThrow().categoryId()).isNull();
        assertThat(purchases.findById(id).orElseThrow().note()).isEqualTo("dela");
    }

    @Test
    @DisplayName("RF-77: the same member and category are applied to selected items, then validated together")
    void batchActions() {
        long a = pending("LOJA A", "10.00");
        long b = pending("LOJA B", "20.00");
        queue.apply(List.of(a, b), edit(null, market), false, null);
        assertThat(queue.item(a).categoryId()).isEqualTo(market);
        assertThat(purchases.findById(a).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.PENDING_REVIEW);
        queue.apply(List.of(a, b), ReviewEdit.none(), true, null);
        assertThat(purchases.findById(b).orElseThrow().reviewStatus()).isEqualTo(ReviewStatus.REVIEWED);
    }

    @Test
    @DisplayName("API-04: editing an outdated review item is refused")
    void staleItem() {
        long id = pending("LOJA C", "10.00");
        assertThatThrownBy(() -> queue.apply(List.of(id), edit(null, market), false, 99L))
                .isInstanceOf(StaleVersionException.class);
    }

    @Test
    @DisplayName("RN-23: a single charge of the same store in the previous period suggests a subscription")
    void subscriptionSuggestion() {
        ledger.addPurchase(1L, f.purchase("MP *PARAMOUNTPLUS", "29.66", LocalDate.of(2026, 8, 27), OCT.previous(), null, null, "PURCHASE"));
        long current = ledger.addPurchase(1L, f.purchase("MP *PARAMOUNTPLUS", "29.90", LocalDate.of(2026, 9, 27), OCT, null, null, "PURCHASE"))
                .purchase().id();
        long unrelated = pending("POSTO", "200.00");
        assertThat(queue.item(current).subscriptionSuggested()).isTrue();
        assertThat(queue.item(unrelated).subscriptionSuggested()).isFalse();
        queue.apply(List.of(current), new ReviewEdit(null, market, false, null, true), true, null);
        assertThat(queue.item(current).installment()).isEqualTo("*");
    }
}

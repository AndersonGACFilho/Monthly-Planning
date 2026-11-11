package com.planejamentomensal.entry.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.category.application.SaveCategoryUseCase;
import com.planejamentomensal.category.domain.CategoryDetails;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.PurchaseType;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SubscriptionCategoryIT {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);
    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private SaveCategoryUseCase categories;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands commands;
    @Autowired private BillEntryRepository entries;
    @Autowired private PurchaseRepository purchases;

    private LedgerFixture f;
    private long streaming;
    private long leisure;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "6");
        streaming = categories.create(new CategoryDetails("Streaming", null, "#6366F1", "repeat", true)).id();
        leisure = categories.create(new CategoryDetails("Lazer", null, "#A855F7", "party-popper", false)).id();
    }

    private NewPurchase purchase(String description, Integer n, Integer count, String kind, Long categoryId) {
        NewPurchase p = f.purchase(description, kind.equals("CREDIT") ? "-39.90" : "39.90", DAY, NOV, n, count, kind);
        return new NewPurchase(p.cardId(), p.cardLastDigits(), p.unknownFinal(), p.purchaseDate(), p.description(),
                p.installmentNumber(), p.installmentCount(), p.amount(), p.kind(), p.bankCategory(), p.note(), p.period(),
                p.entryDate(), p.occurrence(), p.memberId(), categoryId, p.reviewed());
    }

    private PurchaseType type(BillEntry entry) {
        return purchases.findById(entry.purchase().id()).orElseThrow().type();
    }

    @Test
    @DisplayName("IMP-65, RN-09: a single purchase in a subscription category becomes a subscription and is projected")
    void newPurchaseBecomesSubscription() {
        BillEntry entry = ledger.addPurchase(7L, purchase("NETFLIX.COM", null, null, "PURCHASE", streaming));
        assertThat(type(entry)).isEqualTo(PurchaseType.SUBSCRIPTION);
        assertThat(entries.findByPurchaseIdAndStatus(entry.purchase().id(), EntryStatus.PROJECTED))
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.amount()).isEqualTo(Money.of("39.90")));
        ledger.undo(7L);
        assertThat(entries.findByPurchaseIdOrderByEntryDate(entry.purchase().id())).isEmpty();
    }

    @Test
    @DisplayName("IMP-65: installments, credits and other categories keep their type")
    void othersKeepTheirType() {
        assertThat(type(ledger.addPurchase(null, purchase("CURSO ONLINE", 1, 3, "PURCHASE", streaming))))
                .isEqualTo(PurchaseType.INSTALLMENT);
        assertThat(type(ledger.addPurchase(null, purchase("CINEMA", null, null, "PURCHASE", leisure))))
                .isEqualTo(PurchaseType.SINGLE);
        assertThat(type(ledger.addPurchase(null, purchase("ESTORNO STREAMING", null, null, "CREDIT", streaming))))
                .isEqualTo(PurchaseType.SINGLE);
    }

    @Test
    @DisplayName("IMP-65: classifying a single purchase into a subscription category makes it a subscription")
    void classifyingMakesSubscription() {
        BillEntry entry = ledger.addPurchase(null, purchase("SPOTIFY", null, null, "PURCHASE", null));
        assertThat(type(entry)).isEqualTo(PurchaseType.SINGLE);
        commands.classify(entry.id(), streaming, false, null, null, null);
        assertThat(type(entry)).isEqualTo(PurchaseType.SUBSCRIPTION);
        assertThat(entries.findByPurchaseIdAndStatus(entry.purchase().id(), EntryStatus.PROJECTED)).isNotEmpty();
    }
}

package com.planejamentomensal.entry.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.EntryKind;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.PurchaseType;
import com.planejamentomensal.entry.domain.RefundStatus;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PurchaseFacts;
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
class LedgerScenariosIT {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private BillEntryRepository entries;
    @Autowired private PurchaseRepository purchases;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "1");
    }

    @Test
    @DisplayName("RN-26, RN-29: full refund links to the purchase, inherits member and cancels remaining installments; undo restores")
    void fullRefund() {
        BillEntry tablet = ledger.addPurchase(1L, f.purchase("KAZZU AZZEE CALCADOS", "100.00", LocalDate.of(2026, 9, 1), OCT, 1, 3, "PURCHASE"));
        assertThat(entries.findByPurchaseIdAndStatus(tablet.purchase().id(), EntryStatus.PROJECTED)).hasSize(2);
        BillEntry refund = ledger.addPurchase(2L, f.purchase("KAZZU AZZEE CALCADOS", "-300.00", LocalDate.of(2026, 9, 10), OCT, null, null, "CREDIT"));
        assertThat(refund.purchase().kind()).isEqualTo(EntryKind.REFUND);
        assertThat(refund.purchase().refundOfPurchaseId()).isEqualTo(tablet.purchase().id());
        assertThat(purchases.findById(tablet.purchase().id()).orElseThrow().refundStatus()).isEqualTo(RefundStatus.FULL);
        assertThat(entries.findByPurchaseIdAndStatus(tablet.purchase().id(), EntryStatus.PROJECTED)).isEmpty();
        ledger.undo(2L);
        assertThat(purchases.findById(tablet.purchase().id()).orElseThrow().refundStatus()).isEqualTo(RefundStatus.NONE);
        assertThat(entries.findByPurchaseIdAndStatus(tablet.purchase().id(), EntryStatus.PROJECTED)).hasSize(2);
        assertThat(purchases.findById(refund.purchase().id())).isEmpty();
    }

    @Test
    @DisplayName("RN-27: partial refund keeps the remaining installments and shows the balance")
    void partialRefund() {
        BillEntry shoes = ledger.addPurchase(1L, f.purchase("LOJA DE SAPATOS", "200.00", LocalDate.of(2026, 9, 1), OCT, null, null, "PURCHASE"));
        ledger.addPurchase(1L, f.purchase("LOJA DE SAPATOS", "-50.00", LocalDate.of(2026, 9, 5), OCT, null, null, "CREDIT"));
        var original = purchases.findById(shoes.purchase().id()).orElseThrow();
        assertThat(original.refundStatus()).isEqualTo(RefundStatus.PARTIAL);
        assertThat(original.remainingAfterRefunds()).isEqualTo(Money.of("150.00"));
    }

    @Test
    @DisplayName("RN-21: partial entry dated after the closing date moves to the next bill; earlier ones become unconfirmed")
    void reconcileMissing() {
        BillEntry late = ledger.addPurchase(1L, f.purchase("COMPRA TARDIA", "10.00", LocalDate.of(2026, 10, 30), NOV, null, null, "PURCHASE"));
        BillEntry early = ledger.addPurchase(1L, f.purchase("COMPRA CEDO", "10.00", LocalDate.of(2026, 10, 20), NOV, null, null, "PURCHASE"));
        ledger.reconcileMissing(2L, List.of(late.id(), early.id()), LocalDate.of(2026, 10, 28), true);
        assertThat(entries.findById(late.id()).orElseThrow().cardBill().billingPeriod()).isEqualTo(NOV.next());
        assertThat(entries.findById(early.id()).orElseThrow().status()).isEqualTo(EntryStatus.UNCONFIRMED);
        ledger.undo(2L);
        assertThat(entries.findById(late.id()).orElseThrow().cardBill().billingPeriod()).isEqualTo(NOV);
        assertThat(entries.findById(early.id()).orElseThrow().status()).isEqualTo(EntryStatus.CONFIRMED);
    }

    @Test
    @DisplayName("RN-07: projected entries missing from a bill and partial entries missing from a new open file become unconfirmed (Q12)")
    void unconfirmedProjections() {
        BillEntry first = ledger.addPurchase(1L, f.purchase("PARCELADA", "30.00", LocalDate.of(2026, 9, 1), OCT, 1, 2, "PURCHASE"));
        BillEntry second = entries.findByPurchaseIdAndStatus(first.purchase().id(), EntryStatus.PROJECTED).getFirst();
        ledger.reconcileMissing(2L, List.of(second.id()), LocalDate.of(2026, 11, 3), true);
        assertThat(entries.findById(second.id()).orElseThrow().status()).isEqualTo(EntryStatus.UNCONFIRMED);
        BillEntry open = ledger.addPurchase(3L, f.purchase("PARCIAL", "5.00", LocalDate.of(2026, 10, 10), NOV, null, null, "PURCHASE"));
        ledger.reconcileMissing(4L, List.of(open.id(), second.id()), null, false);
        assertThat(entries.findById(open.id()).orElseThrow().status()).isEqualTo(EntryStatus.UNCONFIRMED);
    }

    @Test
    @DisplayName("RN-09: subscriptions are projected to the horizon, renewed when confirmed and stop when missing")
    void subscriptions() {
        BillingPeriod now = BillingPeriod.ofDueDate(LocalDate.now());
        BillEntry charge = ledger.addPurchase(1L, f.purchase("MP *PARAMOUNTPLUS", "29.66", LocalDate.now().minusDays(5), now, null, null, "PURCHASE"));
        ledger.makeSubscription(charge.purchase().id());
        List<BillEntry> projected = entries.findByPurchaseIdAndStatus(charge.purchase().id(), EntryStatus.PROJECTED);
        assertThat(projected).hasSize(12);
        assertThat(projected.getFirst().installmentLabel()).isEqualTo("*");
        assertThat(purchases.findById(charge.purchase().id()).orElseThrow().type()).isEqualTo(PurchaseType.SUBSCRIPTION);
        BillEntry next = projected.stream().filter(e -> e.cardBill().billingPeriod().equals(now.next())).findFirst().orElseThrow();
        ledger.confirmProjected(2L, next.id(), Money.of("29.90"), null);
        assertThat(entries.findByPurchaseIdAndStatus(charge.purchase().id(), EntryStatus.PROJECTED)).hasSize(11);
        ledger.reconcileMissing(3L, List.of(projected.get(1).id()), LocalDate.now().plusMonths(2), true);
        assertThat(entries.findByPurchaseIdAndStatus(charge.purchase().id(), EntryStatus.PROJECTED)).isEmpty();
        assertThat(ledger.entriesForMatching(f.card)).anyMatch(StoredEntry::subscription);
    }

    @Test
    @DisplayName("RN-20, RF-63e: bank amounts update projections and IOF links to its purchase")
    void futureAndIof() {
        BillEntry first = ledger.addPurchase(1L, f.purchase("MERCADOLIVRE*M", "48.02", LocalDate.of(2026, 8, 31), OCT, 1, 2, "PURCHASE"));
        ledger.applyFutureInstallments(2L, f.card, List.of(new PurchaseFacts(f.card, LocalDate.of(2026, 8, 31),
                "MERCADOLIVRE*M", 2, 2, Money.of("48.01"))), OCT);
        assertThat(entries.findByPurchaseIdAndStatus(first.purchase().id(), EntryStatus.PROJECTED).getFirst().amount())
                .isEqualTo(Money.of("48.01"));
        long iof = ledger.recordPurchase(1L, f.purchase("Iof Internacional - X", "3.72", LocalDate.of(2026, 9, 14), OCT, null, null, "IOF"));
        ledger.relate(iof, first.id());
        assertThat(entries.findById(iof).orElseThrow().relatedEntryId()).isEqualTo(first.id());
        ledger.overwrite(2L, first.id(), Money.of("50.00"), LocalDate.of(2026, 9, 1));
        assertThat(entries.findById(first.id()).orElseThrow().amount()).isEqualTo(Money.of("50.00"));
        assertThat(EntryLedger.sameDescription("Bmb *Equato", "BMB EQUATO")).isTrue();
    }
}

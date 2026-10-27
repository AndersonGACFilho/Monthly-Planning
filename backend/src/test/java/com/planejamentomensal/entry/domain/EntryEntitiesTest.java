package com.planejamentomensal.entry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.planejamentomensal.shared.domain.Ids.withId;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntryEntitiesTest {

    static Purchase purchase(String amount, int count, PurchaseType type) {
        return new Purchase(new PurchaseData(1, "4102", LocalDate.of(2026, 9, 20), "BMB *Equato", Money.of(amount), count,
                1, type, EntryKind.PURCHASE, "outros", "key", null, null, ReviewStatus.PENDING_REVIEW, false,
                List.of(new PurchaseSplit(5, new BigDecimal("100"), null))));
    }

    @Test
    @DisplayName("RN-08: category, split and note belong to the purchase and hold for every installment")
    void classify() {
        Purchase purchase = purchase("276.77", 2, PurchaseType.INSTALLMENT);
        assertThat(purchase.normalizedDescription()).isEqualTo("BMBEQUATO");
        assertThat(purchase.totalAmount()).isEqualTo(Money.of("553.54"));
        purchase.classify(9L, List.of(new PurchaseSplit(6, new BigDecimal("50"), null),
                new PurchaseSplit(5, new BigDecimal("50"), null)), "casa", true);
        purchase.markReviewed();
        assertThat(purchase.categoryId()).isEqualTo(9L);
        assertThat(purchase.splits()).hasSize(2);
        assertThat(purchase.manuallyEdited()).isTrue();
        assertThat(purchase.reviewStatus()).isEqualTo(ReviewStatus.REVIEWED);
        assertThat(purchase.note()).isEqualTo("casa");
        assertThatThrownBy(() -> purchase.classify(1L, List.of(), null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RN-26, RN-27: refunds mark the purchase as partially or fully refunded and can be undone")
    void refunds() {
        Purchase purchase = purchase("100.00", 1, PurchaseType.SINGLE);
        purchase.registerRefund(Money.of("-40.00"));
        assertThat(purchase.refundStatus()).isEqualTo(RefundStatus.PARTIAL);
        assertThat(purchase.remainingAfterRefunds()).isEqualTo(Money.of("60.00"));
        purchase.registerRefund(Money.of("60.00"));
        assertThat(purchase.refundStatus()).isEqualTo(RefundStatus.FULL);
        purchase.unregisterRefund(Money.of("60.00"));
        assertThat(purchase.refundStatus()).isEqualTo(RefundStatus.PARTIAL);
        purchase.unregisterRefund(Money.of("40.00"));
        assertThat(purchase.refundStatus()).isEqualTo(RefundStatus.NONE);
        Purchase credit = purchase("-100.00", 1, PurchaseType.SINGLE);
        credit.linkAsRefundOf(3L);
        assertThat(credit.kind()).isEqualTo(EntryKind.REFUND);
        assertThat(credit.refundOfPurchaseId()).isEqualTo(3L);
        purchase.changeType(PurchaseType.SUBSCRIPTION);
        assertThat(purchase.type()).isEqualTo(PurchaseType.SUBSCRIPTION);
    }

    @Test
    @DisplayName("RN-06, RN-07: projected entries become confirmed or unconfirmed and can be restored")
    void entryLifeCycle() {
        Purchase purchase = purchase("48.02", 2, PurchaseType.INSTALLMENT);
        CardBill bill = withId(new CardBill(1, BillingPeriod.of(2026, 10)), 1);
        BillEntry entry = withId(new BillEntry(purchase, bill, 2, LocalDate.of(2026, 8, 31), Money.of("48.02"),
                EntryStatus.PROJECTED, "k|2", 10L), 2);
        assertThat(entry.installmentLabel()).isEqualTo("02/02");
        entry.confirm(Money.of("48.01"), LocalDate.of(2026, 8, 30), 11L);
        assertThat(entry.status()).isEqualTo(EntryStatus.CONFIRMED);
        assertThat(entry.amount()).isEqualTo(Money.of("48.01"));
        assertThat(entry.importBatchId()).isEqualTo(11L);
        BatchChange change = BatchChange.entryChanged(11L, entry);
        entry.markUnconfirmed();
        assertThat(entry.status()).isEqualTo(EntryStatus.UNCONFIRMED);
        CardBill next = new CardBill(1, BillingPeriod.of(2026, 11));
        entry.moveTo(next);
        entry.adjustAmount(Money.of("50.00"));
        entry.relateTo(4L);
        entry.restore(change.previousStatus(), change.previousAmount(), change.previousDate(), bill, change.previousBatchId());
        assertThat(entry.status()).isEqualTo(EntryStatus.CONFIRMED);
        assertThat(entry.amount()).isEqualTo(Money.of("48.01"));
        assertThat(entry.cardBill()).isSameAs(bill);
        assertThat(entry.relatedEntryId()).isEqualTo(4L);
        entry.confirmManually();
        Purchase subscription = purchase("29.66", 1, PurchaseType.SUBSCRIPTION);
        assertThat(new BillEntry(subscription, bill, null, LocalDate.now(), Money.of("29.66"), EntryStatus.PROJECTED,
                "s", null).installmentLabel()).isEqualTo("*");
        assertThat(new BillEntry(purchase("1", 1, PurchaseType.SINGLE), bill, null, LocalDate.now(), Money.of("1"),
                EntryStatus.CONFIRMED, "x", null).installmentLabel()).isEqualTo("-");
    }

    @Test
    @DisplayName("RF-36, RN-22: bills record file dates, become closed and then paid")
    void billLifeCycle() {
        CardBill bill = withId(new CardBill(1, BillingPeriod.of(2026, 11)), 3);
        bill.forecastClosing(LocalDate.of(2026, 11, 2));
        assertThat(bill.closingDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        bill.recordFile(null, LocalDate.of(2026, 11, 9), Money.of("11916.21"), false);
        assertThat(bill.partial()).isTrue();
        assertThat(bill.status()).isEqualTo(BillStatus.OPEN);
        assertThatThrownBy(bill::markPaid).isInstanceOf(IllegalStateException.class);
        BatchChange before = BatchChange.billChanged(3L, bill);
        bill.recordFile(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9), Money.of("12000.00"), true);
        assertThat(bill.status()).isEqualTo(BillStatus.CLOSED);
        assertThat(bill.partial()).isFalse();
        bill.markPaid();
        assertThat(bill.status()).isEqualTo(BillStatus.PAID);
        bill.restore(before.previousBillStatus(), before.previousPartial(), before.previousClosing(), before.previousDue(),
                before.previousAmount());
        assertThat(bill.status()).isEqualTo(BillStatus.OPEN);
        assertThat(bill.fileTotal()).isEqualTo(Money.of("11916.21"));
    }

    @Test
    @DisplayName("RF-72: batch changes keep what is needed to undo a removal and a refund")
    void batchChanges() {
        Purchase purchase = withId(purchase("10.00", 3, PurchaseType.INSTALLMENT), 8);
        BillEntry entry = withId(new BillEntry(purchase, withId(new CardBill(1, BillingPeriod.of(2026, 12)), 4), 3,
                LocalDate.now(), Money.of("10.00"), EntryStatus.PROJECTED, "k|3", 1L), 5);
        BatchChange removed = BatchChange.entryRemoved(2L, entry);
        assertThat(removed.type()).isEqualTo(BatchChange.Type.ENTRY_REMOVED);
        assertThat(removed.removedInstallment()).isEqualTo(3);
        assertThat(removed.removedKey()).isEqualTo("k|3");
        BatchChange refund = BatchChange.refundRegistered(2L, 9L, Money.of("5.00"));
        assertThat(refund.targetId()).isEqualTo(9L);
        assertThat(BatchChange.created(2L, BatchChange.Type.PURCHASE_CREATED, 4L).batchId()).isEqualTo(2L);
    }
}

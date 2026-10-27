package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.BatchChange;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.entry.domain.EntryKeyFactory;
import com.planejamentomensal.entry.domain.EntryKind;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.InstallmentProjector;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseData;
import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.entry.domain.PurchaseType;
import com.planejamentomensal.entry.domain.RefundMatcher;
import com.planejamentomensal.entry.domain.RefundStatus;
import com.planejamentomensal.entry.domain.ReviewStatus;
import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.PurchaseFacts;
import com.planejamentomensal.shared.domain.PurchaseMatcher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only writer of purchases, entries and bills on behalf of imports, recording every change in the
 * batch's log so a batch can be undone exactly (RF-72).
 *
 * <p>Implements RN-04 to RN-09 (projection and reconciliation of installments and subscriptions), RN-20
 * (bank's future installments), RN-21 (open bill followed by the closed one), RN-26 to RN-29 (refunds) and
 * RF-74 (new purchases enter the review queue).</p>
 *
 * @since 1.0
 */
@Service
@Transactional
public class EntryLedger {

    private final CardBillRepository bills;
    private final PurchaseRepository purchases;
    private final BillEntryRepository entries;
    private final BatchChangeRepository changes;
    private final Settings settings;

    /**
     * Creates the ledger.
     *
     * @param bills     bill storage
     * @param purchases purchase storage
     * @param entries   entry storage
     * @param changes   batch log storage
     * @param settings  user settings (projection horizon)
     */
    public EntryLedger(CardBillRepository bills, PurchaseRepository purchases, BillEntryRepository entries,
            BatchChangeRepository changes, Settings settings) {
        this.bills = bills;
        this.purchases = purchases;
        this.entries = entries;
        this.changes = changes;
        this.settings = settings;
    }

    /**
     * Lists the entries of a card for import classification.
     *
     * @param cardId primary card
     * @return stored entries
     */
    @Transactional(readOnly = true)
    public List<StoredEntry> entriesForMatching(long cardId) {
        return entries.findByCardBillCardId(cardId).stream().map(EntryLedger::stored).toList();
    }

    /**
     * Records what an imported file says about a bill (RN-22).
     *
     * @param batchId     batch
     * @param cardId      primary card
     * @param period      billing period
     * @param closing     closing date, or {@code null}
     * @param due         due date
     * @param total       total printed on the file
     * @param closed      {@code true} for a closed bill (PDF)
     * @param nextClosing forecast of the next closing date, or {@code null}
     */
    public void recordBillFile(long batchId, long cardId, BillingPeriod period, LocalDate closing, LocalDate due,
            Money total, boolean closed, LocalDate nextClosing) {
        CardBill bill = bill(batchId, cardId, period);
        changes.save(BatchChange.billChanged(batchId, bill));
        bill.recordFile(closing, due, total, closed);
        bills.save(bill);
        if (nextClosing != null) {
            CardBill next = bill(batchId, cardId, period.next());
            changes.save(BatchChange.billChanged(batchId, next));
            next.forecastClosing(nextClosing);
            bills.save(next);
        }
    }

    /**
     * Records a new purchase with its entry and, for installments, the projected next installments
     * (RN-04, RN-05); a negative line is linked to the purchase it refunds when there is one (RN-26 to RN-29).
     *
     * @param batchId  batch, or {@code null} for a manual entry
     * @param purchase what to record
     * @return the entry created on the bill
     */
    public BillEntry addPurchase(Long batchId, NewPurchase purchase) {
        boolean installment = purchase.installmentNumber() != null && purchase.installmentCount() != null;
        int count = installment ? purchase.installmentCount() : 1;
        int number = installment ? purchase.installmentNumber() : 1;
        String purchaseKey = EntryKeyFactory.purchaseKey(purchase.cardId(), purchase.purchaseDate(), purchase.description(),
                purchase.amount(), count, purchase.occurrence());
        EntryKind kind = EntryKind.valueOf(purchase.kind());
        Purchase created = purchases.save(new Purchase(new PurchaseData(purchase.cardId(), purchase.cardLastDigits(),
                purchase.purchaseDate(), purchase.description(), purchase.amount(), count, number,
                installment ? PurchaseType.INSTALLMENT : PurchaseType.SINGLE, kind, purchase.bankCategory(), purchaseKey,
                purchase.categoryId(), purchase.note(), purchase.reviewed() ? ReviewStatus.REVIEWED : ReviewStatus.PENDING_REVIEW,
                purchase.unknownFinal(), List.of(new PurchaseSplit(purchase.memberId(), BigDecimal.valueOf(100), null)))));
        log(batchId, BatchChange.created(batchIdOrZero(batchId), BatchChange.Type.PURCHASE_CREATED, created.id()));
        if (kind == EntryKind.CREDIT) {
            linkRefund(batchId, created);
        }
        CardBill bill = bill(batchId, purchase.cardId(), purchase.period());
        BillEntry entry = createEntry(batchId, created, bill, installment ? number : null, purchase.entryDate(),
                purchase.amount(), EntryStatus.CONFIRMED, EntryKeyFactory.installmentKey(purchaseKey, installment ? number : null, null));
        if (installment) {
            for (InstallmentProjector.Projection next : InstallmentProjector.remainingInstallments(number, count, purchase.period())) {
                createEntry(batchId, created, bill(batchId, purchase.cardId(), next.period()), next.installmentNumber(),
                        purchase.entryDate().plusMonths(next.installmentNumber() - (long) number), purchase.amount(),
                        EntryStatus.PROJECTED, EntryKeyFactory.installmentKey(purchaseKey, next.installmentNumber(), null));
            }
        }
        return entry;
    }

    /**
     * Records a new purchase for another module and returns only the entry id (SOL-06).
     *
     * @param batchId  batch, or {@code null} for a manual entry
     * @param purchase what to record
     * @return id of the entry created on the bill
     * @see #addPurchase(Long, NewPurchase)
     */
    public long recordPurchase(Long batchId, NewPurchase purchase) {
        return addPurchase(batchId, purchase).id();
    }

    /**
     * Confirms a projected entry seen on a bill, adjusting amount and date (RN-06); a confirmed subscription
     * charge renews the projection horizon (RN-09).
     *
     * @param batchId batch
     * @param entryId projected entry
     * @param amount  amount on the bill
     * @param date    date on the bill, or {@code null} to keep the projected date
     */
    public void confirmProjected(long batchId, long entryId, Money amount, LocalDate date) {
        BillEntry entry = entry(entryId);
        changes.save(BatchChange.entryChanged(batchId, entry));
        entry.confirm(amount, date, batchId);
        entries.save(entry);
        if (entry.purchase().type() == PurchaseType.SUBSCRIPTION) {
            projectSubscription(batchId, entry.purchase(), entry.cardBill().billingPeriod(), entry.entryDate(), amount);
        }
    }

    /**
     * Replaces amount and date of an existing entry after the user chose to update a divergent line; category,
     * member and alias are never touched (RN-14).
     *
     * @param batchId batch
     * @param entryId entry
     * @param amount  amount on the bill
     * @param date    date on the bill, or {@code null} to keep it
     */
    public void overwrite(long batchId, long entryId, Money amount, LocalDate date) {
        confirmProjected(batchId, entryId, amount, date);
    }

    /**
     * Handles the entries of a bill that the imported file did not contain (RN-07, RN-21, Q12): projected or
     * partial entries become unconfirmed, except partial entries dated after the closing date, which move to
     * the next bill. A subscription that stopped appearing on a closed bill loses its later projections.
     *
     * @param batchId batch
     * @param entryIds entries not matched by any line
     * @param closing  closing date of the imported bill, or {@code null}
     * @param closed   {@code true} for a closed bill
     */
    public void reconcileMissing(long batchId, List<Long> entryIds, LocalDate closing, boolean closed) {
        for (Long id : entryIds) {
            BillEntry entry = entry(id);
            if (entry.status() == EntryStatus.UNCONFIRMED) {
                continue;
            }
            boolean partialEntry = entry.status() == EntryStatus.CONFIRMED;
            if (partialEntry && !closed) {
                changes.save(BatchChange.entryChanged(batchId, entry));
                entry.markUnconfirmed();
            } else if (partialEntry && closing != null && entry.entryDate().isAfter(closing)) {
                changes.save(BatchChange.entryChanged(batchId, entry));
                entry.moveTo(bill(batchId, entry.cardBill().cardId(), entry.cardBill().billingPeriod().next()));
            } else if (closed || entry.status() == EntryStatus.PROJECTED) {
                changes.save(BatchChange.entryChanged(batchId, entry));
                entry.markUnconfirmed();
                if (closed && entry.purchase().type() == PurchaseType.SUBSCRIPTION) {
                    removeProjected(batchId, entry.purchase(), entry.cardBill().billingPeriod());
                }
            }
            entries.save(entry);
        }
    }

    /**
     * Updates projected installments with the amounts the bank announces for the next bills; the bank's value
     * wins (RN-20).
     *
     * @param batchId batch
     * @param cardId  primary card
     * @param future  installments listed as "Compras parceladas - próximas faturas"
     * @param period  billing period of the imported bill
     */
    public void applyFutureInstallments(long batchId, long cardId, List<PurchaseFacts> future, BillingPeriod period) {
        List<BillEntry> projected = entries.findByCardBillCardId(cardId).stream()
                .filter(e -> e.status() == EntryStatus.PROJECTED && e.cardBill().billingPeriod().compareTo(period) > 0)
                .toList();
        for (PurchaseFacts announced : future) {
            projected.stream()
                    .filter(e -> PurchaseMatcher.sameInstallment(facts(e), new PurchaseFacts(announced.cardId(),
                            announced.purchaseDate(), announced.description(), announced.installmentNumber(),
                            announced.installmentCount(), e.amount())))
                    .findFirst()
                    .filter(e -> !e.amount().equals(announced.amount()))
                    .ifPresent(e -> {
                        changes.save(BatchChange.entryChanged(batchId, e));
                        e.adjustAmount(announced.amount());
                        entries.save(e);
                    });
        }
    }

    /**
     * Links an IOF entry to the entry of the purchase it was charged for (RF-63e).
     *
     * @param iofEntryId      IOF entry
     * @param purchaseEntryId purchase entry
     */
    public void relate(long iofEntryId, long purchaseEntryId) {
        BillEntry iof = entry(iofEntryId);
        iof.relateTo(purchaseEntryId);
        entries.save(iof);
    }

    /**
     * Turns a purchase into a subscription and projects its charges (RN-09, RN-23).
     *
     * @param purchaseId purchase
     */
    public void makeSubscription(long purchaseId) {
        Purchase purchase = purchases.findById(purchaseId).orElseThrow(() -> new NotFoundException("purchase", purchaseId));
        purchase.changeType(PurchaseType.SUBSCRIPTION);
        purchases.save(purchase);
        entries.findByPurchaseIdOrderByEntryDate(purchaseId).stream()
                .max(java.util.Comparator.comparing((BillEntry e) -> e.cardBill().billingPeriod()))
                .ifPresent(last -> projectSubscription(null, purchase, last.cardBill().billingPeriod(), last.entryDate(),
                        last.amount()));
    }

    /**
     * Reverts every change of a batch, latest first (RF-72).
     *
     * @param batchId batch
     */
    public void undo(long batchId) {
        for (BatchChange change : changes.findByBatchIdOrderByIdDesc(batchId)) {
            switch (change.type()) {
                case ENTRY_CREATED -> entries.findById(change.targetId()).ifPresent(entries::delete);
                case PURCHASE_CREATED -> purchases.findById(change.targetId()).ifPresent(purchases::delete);
                case ENTRY_CHANGED -> entries.findById(change.targetId()).ifPresent(e -> {
                    e.restore(change.previousStatus(), change.previousAmount(), change.previousDate(),
                            bills.findById(change.previousBillId()).orElse(e.cardBill()), change.previousBatchId());
                    entries.save(e);
                });
                case ENTRY_REMOVED -> purchases.findById(change.removedPurchaseId()).ifPresent(p ->
                        bills.findById(change.previousBillId()).ifPresent(b -> entries.save(new BillEntry(p, b,
                                change.removedInstallment(), change.previousDate(), change.previousAmount(),
                                change.previousStatus(), change.removedKey(), change.previousBatchId()))));
                case REFUND_REGISTERED -> purchases.findById(change.targetId()).ifPresent(p -> {
                    p.unregisterRefund(change.previousAmount());
                    purchases.save(p);
                });
                case BILL_CHANGED -> bills.findById(change.targetId()).ifPresent(b -> {
                    b.restore(change.previousBillStatus(), Boolean.TRUE.equals(change.previousPartial()),
                            change.previousClosing(), change.previousDue(), change.previousAmount());
                    bills.save(b);
                });
                case BILL_CREATED -> bills.findById(change.targetId())
                        .filter(b -> entries.findByCardBillId(b.id()).isEmpty())
                        .ifPresent(bills::delete);
            }
        }
        changes.deleteByBatchId(batchId);
    }

    private void linkRefund(Long batchId, Purchase credit) {
        List<RefundMatcher.Candidate> candidates = purchases.findByCardId(credit.cardId()).stream()
                .filter(p -> !p.id().equals(credit.id()) && !p.installmentAmount().isNegative()
                        && (p.kind() == EntryKind.PURCHASE || p.kind() == EntryKind.FEE))
                .map(p -> new RefundMatcher.Candidate(p.id(), p.cardId(), p.purchaseDate(), p.originalDescription(),
                        p.remainingAfterRefunds()))
                .toList();
        Optional<RefundMatcher.Candidate> original = RefundMatcher.find(credit.cardId(), credit.purchaseDate(),
                credit.originalDescription(), credit.installmentAmount(), candidates);
        original.flatMap(c -> purchases.findById(c.purchaseId())).ifPresent(orig -> {
            credit.linkAsRefundOf(orig.id());
            credit.classify(orig.categoryId(), orig.splits().stream()
                    .map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList(), credit.note(), false);
            purchases.save(credit);
            orig.registerRefund(credit.installmentAmount());
            purchases.save(orig);
            log(batchId, BatchChange.refundRegistered(batchIdOrZero(batchId), orig.id(), credit.installmentAmount().abs()));
            if (orig.refundStatus() == RefundStatus.FULL) {
                removeProjected(batchId, orig, null);
            }
        });
    }

    private void removeProjected(Long batchId, Purchase purchase, BillingPeriod after) {
        for (BillEntry projected : entries.findByPurchaseIdAndStatus(purchase.id(), EntryStatus.PROJECTED)) {
            if (after == null || projected.cardBill().billingPeriod().compareTo(after) > 0) {
                log(batchId, BatchChange.entryRemoved(batchIdOrZero(batchId), projected));
                entries.delete(projected);
            }
        }
    }

    private void projectSubscription(Long batchId, Purchase purchase, BillingPeriod from, LocalDate lastDate, Money amount) {
        BillingPeriod horizonEnd = BillingPeriod.ofDueDate(LocalDate.now())
                .plusMonths(settings.getInt(Setting.PROJECTION_HORIZON_MONTHS));
        List<BillingPeriod> existing = entries.findByPurchaseIdOrderByEntryDate(purchase.id()).stream()
                .map(e -> e.cardBill().billingPeriod()).toList();
        for (InstallmentProjector.Projection charge : InstallmentProjector.subscriptionCharges(from, horizonEnd)) {
            if (!existing.contains(charge.period())) {
                createEntry(batchId, purchase, bill(batchId, purchase.cardId(), charge.period()), null,
                        lastDate.plusMonths(from.monthsUntil(charge.period())), amount, EntryStatus.PROJECTED,
                        EntryKeyFactory.installmentKey(purchase.purchaseKey(), null, charge.period().toString()));
            }
        }
    }

    private BillEntry createEntry(Long batchId, Purchase purchase, CardBill bill, Integer installment, LocalDate date,
            Money amount, EntryStatus status, String key) {
        BillEntry entry = entries.save(new BillEntry(purchase, bill, installment, date, amount, status, key, batchId));
        log(batchId, BatchChange.created(batchIdOrZero(batchId), BatchChange.Type.ENTRY_CREATED, entry.id()));
        return entry;
    }

    private CardBill bill(Long batchId, long cardId, BillingPeriod period) {
        return bills.findByCardIdAndBillingPeriod(cardId, period).orElseGet(() -> {
            CardBill created = bills.save(new CardBill(cardId, period));
            log(batchId, BatchChange.created(batchIdOrZero(batchId), BatchChange.Type.BILL_CREATED, created.id()));
            return created;
        });
    }

    private void log(Long batchId, BatchChange change) {
        if (batchId != null) {
            changes.save(change);
        }
    }

    private static long batchIdOrZero(Long batchId) {
        return batchId == null ? 0 : batchId;
    }

    private BillEntry entry(long id) {
        return entries.findById(id).orElseThrow(() -> new NotFoundException("bill-entry", id));
    }

    private static StoredEntry stored(BillEntry e) {
        return new StoredEntry(e.id(), e.purchase().id(), e.installmentKey(), e.status().name(), facts(e),
                e.cardBill().billingPeriod(), e.purchase().type() == PurchaseType.SUBSCRIPTION, e.importBatchId());
    }

    private static PurchaseFacts facts(BillEntry e) {
        Purchase p = e.purchase();
        boolean subscription = p.type() == PurchaseType.SUBSCRIPTION;
        boolean installment = p.type() == PurchaseType.INSTALLMENT;
        return new PurchaseFacts(e.cardBill().cardId(), subscription ? e.entryDate() : p.purchaseDate(),
                p.originalDescription(), installment ? e.installmentNumber() : null,
                installment ? p.installmentCount() : null, e.amount());
    }

    /**
     * Tells whether a description is the same store as a purchase, used by IOF linking (RF-63e).
     *
     * @param a first description
     * @param b second description
     * @return {@code true} for the same store or equal normalized text
     */
    public static boolean sameDescription(String a, String b) {
        return DescriptionNormalizer.normalize(a).equals(DescriptionNormalizer.normalize(b));
    }
}

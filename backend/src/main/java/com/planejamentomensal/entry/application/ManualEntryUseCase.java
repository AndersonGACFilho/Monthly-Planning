package com.planejamentomensal.entry.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.card.application.CycleView;
import com.planejamentomensal.entry.domain.BillingPeriodPolicy;
import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.entry.domain.RefundStatus;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Versions;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records, replaces and deletes purchases typed by the user (RF-44, RF-45, RF-79c, RN-01, RN-22).
 *
 * <p>A manual purchase can only be replaced or deleted while no imported bill has seen any of its
 * installments and no refund points to it: after that the bank's data owns it.</p>
 *
 * @since 1.0
 */
@Service
@Transactional
public class ManualEntryUseCase {

    private final EntryQueries queries;
    private final PurchaseRepository purchases;
    private final BillEntryRepository entries;
    private final CardBillRepository bills;
    private final EntryLedger ledger;
    private final CardQueries cards;

    /**
     * Creates the use case.
     *
     * @param queries   entry queries
     * @param purchases purchase storage
     * @param entries   entry storage
     * @param bills     bill storage, for known closing dates
     * @param ledger    ledger that records purchases and their installments
     * @param cards     card queries, for cycles
     */
    public ManualEntryUseCase(EntryQueries queries, PurchaseRepository purchases, BillEntryRepository entries,
            CardBillRepository bills, EntryLedger ledger, CardQueries cards) {
        this.queries = queries;
        this.purchases = purchases;
        this.entries = entries;
        this.bills = bills;
        this.ledger = ledger;
        this.cards = cards;
    }

    /**
     * Records a purchase, already validated, on the bill the user chose or, without one, on the bill the card's
     * cycle and known closing dates give; installment purchases project the next installments.
     *
     * @param purchase what to record
     * @return the first entry
     */
    public EntryRow create(ManualPurchase purchase) {
        CardView card = cards.find(purchase.cardId())
                .orElseThrow(() -> new InvalidInputException("import-card-unknown", purchase.cardId()));
        long billingCard = card.billingCardId();
        BillingPeriod period = purchase.billingPeriod() != null
                ? purchase.billingPeriod()
                : periodByCycle(billingCard, purchase.cardId(), purchase.purchaseDate());
        boolean installments = purchase.installmentCount() > 1;
        long entryId = ledger.recordPurchase(null, new NewPurchase(billingCard, card.lastDigits().getFirst(), false,
                purchase.purchaseDate(), purchase.description().strip(), installments ? 1 : null,
                installments ? purchase.installmentCount() : null, purchase.installmentAmount(),
                purchase.installmentAmount().isNegative() ? "CREDIT" : "PURCHASE", null, purchase.note(), period,
                purchase.purchaseDate(), 1, purchase.memberId() != null ? purchase.memberId() : card.defaultMemberId(),
                purchase.categoryId(), true));
        return queries.get(entryId);
    }

    /**
     * Replaces the purchase of a manual entry, moving its installments to the bills the new data gives; without a
     * member the current split is kept.
     *
     * @param entryId         any entry of the purchase
     * @param purchase        new data
     * @param expectedVersion purchase version the client edited, or {@code null}
     * @return the first entry of the new purchase
     */
    public EntryRow replace(long entryId, ManualPurchase purchase, Long expectedVersion) {
        Purchase current = editable(entryId, expectedVersion);
        List<PurchaseSplit> splits = current.splits().stream()
                .map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList();
        remove(current);
        EntryRow created = create(purchase);
        if (purchase.memberId() == null) {
            Purchase replaced = queries.entry(created.entryId()).purchase();
            replaced.classify(replaced.categoryId(), splits, replaced.note(), true);
            purchases.save(replaced);
            return queries.get(created.entryId());
        }
        return created;
    }

    /**
     * Deletes the purchase of a manual entry with all its installments; bills left empty stay.
     *
     * @param entryId         any entry of the purchase
     * @param expectedVersion purchase version the client saw, or {@code null}
     */
    public void delete(long entryId, Long expectedVersion) {
        remove(editable(entryId, expectedVersion));
    }

    private Purchase editable(long entryId, Long expectedVersion) {
        Purchase purchase = queries.entry(entryId).purchase();
        Versions.check(expectedVersion, purchase.version());
        boolean imported = !entries.findImportedPurchaseIds(List.of(purchase.id())).isEmpty();
        boolean refunded = purchase.refundStatus() != RefundStatus.NONE || purchase.refundOfPurchaseId() != null;
        if (imported || refunded) {
            throw new BusinessRuleException("entry-not-manual", entryId);
        }
        return purchase;
    }

    private void remove(Purchase purchase) {
        entries.findByPurchaseIdOrderByEntryDate(purchase.id()).forEach(entries::delete);
        purchases.delete(purchase);
    }

    private BillingPeriod periodByCycle(long billingCard, long cardId, LocalDate purchaseDate) {
        CycleView cycle = cards.cycleFor(billingCard, BillingPeriod.ofDueDate(purchaseDate))
                .orElseThrow(() -> new InvalidInputException("card-without-cycle", cardId));
        Map<BillingPeriod, LocalDate> known = bills.findByCardId(billingCard).stream()
                .filter(b -> b.closingDate() != null)
                .collect(Collectors.toMap(CardBill::billingPeriod, CardBill::closingDate, (a, b) -> a));
        return BillingPeriodPolicy.firstPeriod(purchaseDate, cycle.closingDay(), cycle.dueDay(), known);
    }
}

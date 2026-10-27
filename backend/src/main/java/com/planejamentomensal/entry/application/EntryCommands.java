package com.planejamentomensal.entry.application;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.card.application.CycleView;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.BillingPeriodPolicy;
import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.Versions;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes made by the user on the card entries tab: inline editing, renaming, restoring the original,
 * confirming a projected installment and manual entries (RF-43 to RF-50, RN-08, RN-14).
 *
 * @since 1.0
 */
@Service
@Transactional
public class EntryCommands {

    /** Scope of a rename (RF-48). */
    public enum RenameScope {
        /** Every entry with the same original description, past and future (default). */
        ALL,
        /** Only this entry ({@code AliasOverride}). */
        ONLY_THIS
    }

    private final EntryQueries queries;
    private final PurchaseRepository purchases;
    private final BillEntryRepository entries;
    private final CardBillRepository bills;
    private final AliasService aliases;
    private final EntryLedger ledger;
    private final CardQueries cards;

    /**
     * Creates the commands.
     *
     * @param queries   entry queries
     * @param purchases purchase storage
     * @param entries   entry storage
     * @param bills     bill storage
     * @param aliases   alias service
     * @param ledger    ledger, for manual purchases
     * @param cards     card queries, for cycles
     */
    public EntryCommands(EntryQueries queries, PurchaseRepository purchases, BillEntryRepository entries,
            CardBillRepository bills, AliasService aliases, EntryLedger ledger, CardQueries cards) {
        this.queries = queries;
        this.purchases = purchases;
        this.entries = entries;
        this.bills = bills;
        this.aliases = aliases;
        this.ledger = ledger;
        this.cards = cards;
    }

    /**
     * Changes category, member split or note of an entry's purchase; reimports never overwrite it (RF-43,
     * RN-08, RN-14).
     *
     * @param entryId         entry
     * @param categoryId      category, or {@code null} to keep
     * @param clearCategory   {@code true} to remove the category
     * @param splits          member shares, or {@code null} to keep
     * @param note            note, or {@code null} to keep
     * @param expectedVersion purchase version the client edited, or {@code null}
     * @return the updated row
     */
    public EntryRow classify(long entryId, Long categoryId, boolean clearCategory, List<ReviewItem.Share> splits, String note,
            Long expectedVersion) {
        BillEntry entry = queries.entry(entryId);
        Purchase purchase = entry.purchase();
        Versions.check(expectedVersion, purchase.version());
        List<PurchaseSplit> newSplits = splits == null
                ? purchase.splits().stream().map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList()
                : splits.stream().map(s -> new PurchaseSplit(s.memberId(), s.percentage(), s.amount())).toList();
        purchase.classify(clearCategory ? null : categoryId != null ? categoryId : purchase.categoryId(), newSplits,
                note != null ? (note.isBlank() ? null : note.strip()) : purchase.note(), true);
        purchases.save(purchase);
        return queries.get(entryId);
    }

    /**
     * Renames the description of an entry (RF-46 to RF-49).
     *
     * @param entryId           entry
     * @param displayName       new name
     * @param scope             all entries with the description, or only this one
     * @param defaultCategoryId category for future imports, or {@code null}
     * @return the updated row
     */
    public EntryRow rename(long entryId, String displayName, RenameScope scope, Long defaultCategoryId) {
        BillEntry entry = queries.entry(entryId);
        if (scope == RenameScope.ONLY_THIS) {
            aliases.renameOne(entryId, displayName);
        } else {
            aliases.renameAll(entry.purchase().originalDescription(), displayName, defaultCategoryId);
        }
        return queries.get(entryId);
    }

    /**
     * Shows the card's description again (RF-50).
     *
     * @param entryId entry
     * @return the updated row
     */
    public EntryRow restoreOriginal(long entryId) {
        BillEntry entry = queries.entry(entryId);
        aliases.restoreOriginal(entryId, entry.purchase().originalDescription());
        return queries.get(entryId);
    }

    /**
     * Confirms a projected installment by hand (link {@code confirm}).
     *
     * @param entryId entry
     * @return the updated row
     * @throws BusinessRuleException with code {@code entry-not-projected} if it is not projected
     */
    public EntryRow confirm(long entryId) {
        BillEntry entry = queries.entry(entryId);
        if (entry.status() != EntryStatus.PROJECTED) {
            throw new BusinessRuleException("entry-not-projected", entryId);
        }
        entry.confirmManually();
        entries.save(entry);
        return queries.get(entryId);
    }

    /**
     * Records a purchase that was not imported, already validated; the billing period follows the card's
     * cycle and known closing dates, and installment purchases project the next installments (RF-44, RF-45,
     * RF-79c, RN-01, RN-22).
     *
     * @param cardId           card (an additional card resolves to its primary card)
     * @param purchaseDate     purchase date
     * @param description      description
     * @param installmentAmount amount of each installment
     * @param installmentCount number of installments, 1 for a single purchase
     * @param memberId         member, or {@code null} for the card's default member
     * @param categoryId       category, or {@code null}
     * @param note             note, or {@code null}
     * @return the first entry
     */
    public EntryRow addManual(long cardId, LocalDate purchaseDate, String description, Money installmentAmount,
            int installmentCount, Long memberId, Long categoryId, String note) {
        CardView card = cards.find(cardId).orElseThrow(() -> new InvalidInputException("import-card-unknown", cardId));
        long billingCard = card.billingCardId();
        BillingPeriod month = BillingPeriod.ofDueDate(purchaseDate);
        CycleView cycle = cards.cycleFor(billingCard, month).orElseThrow(() -> new InvalidInputException("card-without-cycle", cardId));
        Map<BillingPeriod, LocalDate> known = bills.findByCardId(billingCard).stream()
                .filter(b -> b.closingDate() != null)
                .collect(Collectors.toMap(CardBill::billingPeriod, CardBill::closingDate, (a, b) -> a));
        BillingPeriod period = BillingPeriodPolicy.firstPeriod(purchaseDate, cycle.closingDay(), cycle.dueDay(), known);
        boolean installments = installmentCount > 1;
        long entryId = ledger.recordPurchase(null, new NewPurchase(billingCard, card.lastDigits().getFirst(), false,
                purchaseDate, description.strip(), installments ? 1 : null, installments ? installmentCount : null,
                installmentAmount, installmentAmount.isNegative() ? "CREDIT" : "PURCHASE", null, note, period, purchaseDate,
                1, memberId != null ? memberId : card.defaultMemberId(), categoryId, true));
        return queries.get(entryId);
    }
}

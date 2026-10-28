package com.planejamentomensal.entry.application;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseSplit;
import com.planejamentomensal.entry.domain.SplitPolicy;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Versions;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes made by the user on the card entries tab: inline editing, renaming, restoring the original and
 * confirming a projected installment (RF-43 to RF-50, RN-08, RN-14).
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
    private final AliasService aliases;

    /**
     * Creates the commands.
     *
     * @param queries   entry queries
     * @param purchases purchase storage
     * @param entries   entry storage
     * @param aliases   alias service
     */
    public EntryCommands(EntryQueries queries, PurchaseRepository purchases, BillEntryRepository entries,
            AliasService aliases) {
        this.queries = queries;
        this.purchases = purchases;
        this.entries = entries;
        this.aliases = aliases;
    }

    /**
     * Changes category, member split or note of an entry's purchase; reimports never overwrite it (RF-43,
     * RN-08, RN-14). A new split must assign the whole installment amount (RF-26).
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
        if (splits != null && !SplitPolicy.isComplete(purchase.installmentAmount(), newSplits)) {
            throw new BusinessRuleException("review-split-incomplete", purchase.originalDescription());
        }
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
}

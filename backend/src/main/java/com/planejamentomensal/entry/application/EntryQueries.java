package com.planejamentomensal.entry.application;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.PurchaseType;
import com.planejamentomensal.entry.domain.ReviewStatus;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lists card entries with their displayed descriptions, filters and sort (RF-40, RF-41, RF-51, API-06).
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class EntryQueries {

    private final BillEntryRepository entries;
    private final PurchaseRepository purchases;
    private final AliasService aliases;
    private final CategoryQueries categories;

    /**
     * Creates the queries.
     *
     * @param entries    entry storage
     * @param purchases  purchase storage
     * @param aliases    alias resolution
     * @param categories category queries
     */
    public EntryQueries(BillEntryRepository entries, PurchaseRepository purchases, AliasService aliases,
            CategoryQueries categories) {
        this.entries = entries;
        this.purchases = purchases;
        this.aliases = aliases;
        this.categories = categories;
    }

    /**
     * Lists entries matching a filter, sorted by entry date by default (RF-41).
     *
     * @param filter filters
     * @return rows
     */
    public List<EntryRow> list(EntryFilter filter) {
        List<BillEntry> found = filter.billingPeriod() != null
                ? entries.findByCardBillBillingPeriod(filter.billingPeriod())
                : filter.cardId() != null ? entries.findByCardBillCardId(filter.cardId()) : List.of();
        List<EntryRow> rows = rows(found);
        long unclassified = categories.unclassifiedId();
        String text = filter.text() == null ? null : filter.text().strip().toLowerCase(Locale.ROOT);
        String normalizedText = text == null ? null : DescriptionNormalizer.normalize(text);
        return rows.stream()
                .filter(r -> filter.cardId() == null || r.cardId() == filter.cardId())
                .filter(r -> filter.memberId() == null || r.splits().stream().anyMatch(s -> s.memberId() == filter.memberId()))
                .filter(r -> filter.categoryId() == null || filter.categoryId().equals(r.categoryId())
                        || filter.categoryId() == unclassified && r.categoryId() == null)
                .filter(r -> filter.type() == null || typeMatches(filter.type(), r))
                .filter(r -> filter.status() == null || filter.status().equals(r.status()))
                .filter(r -> text == null || text.isEmpty()
                        || r.displayDescription().toLowerCase(Locale.ROOT).contains(text)
                        || r.originalDescription().toLowerCase(Locale.ROOT).contains(text)
                        || !normalizedText.isEmpty() && DescriptionNormalizer.normalize(r.originalDescription()).contains(normalizedText))
                .sorted(Comparator.comparing(EntryRow::entryDate).thenComparing(EntryRow::entryId))
                .toList();
    }

    /**
     * Returns one entry.
     *
     * @param entryId entry
     * @return the row
     * @throws NotFoundException if it does not exist
     */
    public EntryRow get(long entryId) {
        return rows(List.of(entry(entryId))).getFirst();
    }

    /**
     * Returns the installments of a purchase (link {@code installments}).
     *
     * @param purchaseId purchase
     * @return rows in date order
     */
    public List<EntryRow> installments(long purchaseId) {
        return rows(entries.findByPurchaseIdOrderByEntryDate(purchaseId));
    }

    /**
     * Returns the category last chosen for a description, to suggest it on new entries (RF-08).
     *
     * @param originalDescription original description
     * @return category of the latest reviewed purchase with the same normalized description, or empty
     */
    public Optional<Long> lastCategoryOf(String originalDescription) {
        return purchases.findByNormalizedDescription(DescriptionNormalizer.normalize(originalDescription)).stream()
                .filter(p -> p.reviewStatus() == ReviewStatus.REVIEWED && p.categoryId() != null)
                .max(Comparator.comparing(Purchase::purchaseDate).thenComparing(Purchase::id))
                .map(Purchase::categoryId);
    }

    BillEntry entry(long id) {
        return entries.findById(id).orElseThrow(() -> new NotFoundException("bill-entry", id));
    }

    private List<EntryRow> rows(List<BillEntry> found) {
        Map<Long, AliasService.Display> display = aliases.display(found.stream()
                .collect(Collectors.toMap(BillEntry::id, e -> e.purchase().originalDescription(), (a, b) -> a)));
        return found.stream().map(e -> row(e, display.get(e.id()))).toList();
    }

    private static EntryRow row(BillEntry e, AliasService.Display display) {
        Purchase p = e.purchase();
        return new EntryRow(e.id(), p.id(), e.cardBill().id(), e.cardBill().cardId(), p.cardLastDigits(),
                e.cardBill().billingPeriod(), e.entryDate(), p.purchaseDate(), p.originalDescription(), display.text(),
                display.renamed(), display.aliasId(), e.installmentLabel(), p.type().name(), p.kind().name(), e.amount(),
                e.status().name(), p.categoryId(),
                p.splits().stream().map(s -> new ReviewItem.Share(s.memberId(), s.percentage(), s.amount())).toList(),
                p.reviewStatus().name(), p.refundStatus().name(), p.note(), p.version());
    }

    private static boolean typeMatches(String type, EntryRow row) {
        return switch (type) {
            case "-" -> row.type().equals(PurchaseType.SINGLE.name());
            case "*" -> row.type().equals(PurchaseType.SUBSCRIPTION.name());
            default -> row.type().equals(PurchaseType.INSTALLMENT.name());
        };
    }

    /**
     * Groups rows by id, for callers that look up entries.
     *
     * @param rows rows
     * @return rows by entry id
     */
    public static Map<Long, EntryRow> byId(List<EntryRow> rows) {
        return rows.stream().collect(Collectors.toMap(EntryRow::entryId, Function.identity()));
    }
}

package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.PurchaseFacts;
import com.planejamentomensal.shared.domain.PurchaseMatcher;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Gives each imported line its status (RN-13), in this order:
 * <ol>
 *   <li>an exact or tolerant (RN-19) match with a confirmed entry is a duplicate (also covers the open XLSX
 *   followed by the closed PDF, RN-21);</li>
 *   <li>a match with a projected or unconfirmed entry is a matched installment (RN-06);</li>
 *   <li>same card, store and installment with another amount or date is divergent;</li>
 *   <li>anything else is new.</li>
 * </ol>
 * Each existing entry matches at most one line, so two identical purchases on the same day stay two
 * purchases (RN-12, RN-25). Subscription charges match by store and billing period (RN-09).
 *
 * @since 1.0
 */
public final class ImportClassifier {

    private final List<ExistingEntry> existing;
    private final Set<Long> used = new HashSet<>();

    /**
     * Creates a classifier for one import.
     *
     * @param existing entries of the card already stored
     */
    public ImportClassifier(List<ExistingEntry> existing) {
        this.existing = existing;
    }

    /**
     * Classifies one line; lines must be classified in file order.
     *
     * @param line           the line's facts
     * @param installmentKey the line's exact key (RN-10)
     * @param period         billing period of the import (RN-03)
     * @return status and matched entry
     */
    public Classification classify(PurchaseFacts line, String installmentKey, BillingPeriod period) {
        Optional<ExistingEntry> duplicate = first(e -> e.state() == ExistingEntry.State.CONFIRMED
                && (e.installmentKey().equals(installmentKey) || PurchaseMatcher.sameInstallment(e.facts(), line)));
        if (duplicate.isPresent()) {
            return take(LineStatus.DUPLICATE, duplicate.get());
        }
        Optional<ExistingEntry> projected = first(e -> e.state() != ExistingEntry.State.CONFIRMED
                && (e.installmentKey().equals(installmentKey) || PurchaseMatcher.sameInstallment(e.facts(), line)
                        || subscriptionCharge(e, line, period)));
        if (projected.isPresent()) {
            return take(LineStatus.MATCHED, projected.get());
        }
        Optional<ExistingEntry> divergent = first(e -> line.installmentNumber() != null
                && PurchaseMatcher.divergent(e.facts(), line));
        if (divergent.isPresent()) {
            return take(LineStatus.DIVERGENT, divergent.get());
        }
        return Classification.newLine();
    }

    private static boolean subscriptionCharge(ExistingEntry entry, PurchaseFacts line, BillingPeriod period) {
        return entry.subscription() && line.installmentNumber() == null && entry.period().equals(period)
                && entry.facts().cardId() == line.cardId()
                && (DescriptionNormalizer.normalize(entry.facts().description()).equals(DescriptionNormalizer.normalize(line.description()))
                        || DescriptionNormalizer.sameStore(entry.facts().description(), line.description()));
    }

    private Optional<ExistingEntry> first(Predicate<ExistingEntry> condition) {
        return existing.stream().filter(e -> !used.contains(e.entryId())).filter(condition).findFirst();
    }

    private Classification take(LineStatus status, ExistingEntry entry) {
        used.add(entry.entryId());
        return new Classification(status, entry.entryId());
    }

    /**
     * Returns the entries that no line matched, such as projected installments missing from a closed bill
     * (RN-07, RN-21).
     *
     * @param period billing period of the import
     * @return unmatched entries of that period
     */
    public List<ExistingEntry> unmatchedIn(BillingPeriod period) {
        return existing.stream().filter(e -> !used.contains(e.entryId()) && e.period().equals(period)).toList();
    }
}

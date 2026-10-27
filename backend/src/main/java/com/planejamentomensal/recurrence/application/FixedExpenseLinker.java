package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links fixed expenses paid by card to the card entry that holds them, so they are not counted twice (RF-33):
 * same description by alias or store prefix (RN-16) and an amount at most 10% apart. More than one candidate
 * waits for the user's choice (RN-30).
 *
 * @since 1.0
 */
@Service
@Transactional
public class FixedExpenseLinker {

    /** Largest relative difference between the expense and the card entry. */
    static final BigDecimal CLOSE = new BigDecimal("0.10");

    private final EntryQueries entries;
    private final OccurrenceRepository occurrences;

    /**
     * Creates the linker.
     *
     * @param entries     entry queries
     * @param occurrences occurrence storage
     */
    public FixedExpenseLinker(EntryQueries entries, OccurrenceRepository occurrences) {
        this.entries = entries;
        this.occurrences = occurrences;
    }

    /**
     * Links the unlinked card-paid expenses of a period.
     *
     * @param expenses fixed expense occurrences of one period
     */
    public void link(List<Occurrence> expenses) {
        List<Occurrence> open = expenses.stream()
                .filter(o -> o.rule().paymentMethod() == PaymentMethod.CARD && o.linkedEntryId() == null)
                .toList();
        if (open.isEmpty()) {
            return;
        }
        List<EntryRow> rows = entries.list(new EntryFilter(open.getFirst().billingPeriod(), null, null, null, null, null, null));
        for (Occurrence expense : open) {
            List<EntryRow> candidates = rows.stream()
                    .filter(r -> !"UNCONFIRMED".equals(r.status()))
                    .filter(r -> sameDescription(expense.rule().description(), r))
                    .filter(r -> close(expense.amount(), r.amount()))
                    .toList();
            if (candidates.size() == 1) {
                expense.linkTo(candidates.getFirst().entryId(), candidates.getFirst().entryDate());
            } else if (candidates.size() > 1) {
                expense.offerCandidates(candidates.stream().map(r -> String.valueOf(r.entryId())).collect(Collectors.joining(",")));
            }
            occurrences.saveAndFlush(expense);
        }
    }

    /**
     * Links a fixed expense to the entry the user chose among its candidates (RN-30).
     *
     * @param expense occurrence with candidates
     * @param entryId chosen entry
     * @throws BusinessRuleException with code {@code expense-link-invalid} if the entry is not a candidate
     */
    public void choose(Occurrence expense, long entryId) {
        boolean candidate = expense.linkCandidates() != null
                && Arrays.asList(expense.linkCandidates().split(",")).contains(String.valueOf(entryId));
        if (!candidate) {
            throw new BusinessRuleException("expense-link-invalid", entryId);
        }
        EntryRow row = entries.get(entryId);
        expense.linkTo(row.entryId(), row.entryDate());
        occurrences.saveAndFlush(expense);
    }

    private static boolean sameDescription(String description, EntryRow row) {
        String normalized = DescriptionNormalizer.normalize(description);
        return normalized.equals(DescriptionNormalizer.normalize(row.displayDescription()))
                || normalized.equals(DescriptionNormalizer.normalize(row.originalDescription()))
                || DescriptionNormalizer.sameStore(description, row.originalDescription());
    }

    private static boolean close(Money expected, Money actual) {
        BigDecimal limit = expected.abs().amount().multiply(CLOSE);
        return expected.minus(actual).abs().amount().compareTo(limit) <= 0;
    }
}

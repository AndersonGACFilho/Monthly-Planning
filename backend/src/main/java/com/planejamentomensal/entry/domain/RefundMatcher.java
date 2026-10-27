package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Finds the purchase a negative line refunds (RN-26 to RN-28).
 *
 * <p>A negative line refunds an earlier purchase of the same card and store (RN-16) whose remaining amount
 * is at least the refunded amount (RN-26, RN-27). An exact amount wins; among equals, the most recent
 * purchase. Without a candidate the line is a standalone credit (RN-28).</p>
 *
 * @since 1.0
 */
public final class RefundMatcher {

    /**
     * A purchase that could be refunded.
     *
     * @param purchaseId  purchase
     * @param cardId      card
     * @param date        purchase date
     * @param description original description
     * @param remaining   total minus earlier refunds
     */
    public record Candidate(long purchaseId, long cardId, LocalDate date, String description, Money remaining) {
    }

    private RefundMatcher() {
    }

    /**
     * Picks the refunded purchase.
     *
     * @param cardId      card of the negative line
     * @param date        date of the negative line
     * @param description description of the negative line
     * @param amount      negative amount
     * @param candidates  earlier purchases of the card
     * @return the refunded purchase, or empty for a standalone credit
     */
    public static Optional<Candidate> find(long cardId, LocalDate date, String description, Money amount,
            List<Candidate> candidates) {
        Money refund = amount.abs();
        return candidates.stream()
                .filter(c -> c.cardId() == cardId)
                .filter(c -> !c.date().isAfter(date))
                .filter(c -> !c.remaining().isNegative() && refund.compareTo(c.remaining()) <= 0)
                .filter(c -> DescriptionNormalizer.normalize(c.description()).equals(DescriptionNormalizer.normalize(description))
                        || DescriptionNormalizer.sameStore(c.description(), description))
                .min(Comparator.comparing((Candidate c) -> !c.remaining().equals(refund))
                        .thenComparing(Candidate::date, Comparator.reverseOrder()));
    }
}

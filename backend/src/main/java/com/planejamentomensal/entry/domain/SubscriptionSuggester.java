package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Suggests marking a purchase as a subscription {@code *} (RN-23).
 *
 * <p>Subscriptions are not marked on the bills. A single purchase looks like one when the same store
 * (RN-16), without installments, was charged in the previous billing period with a similar amount; the
 * user confirms. "Similar" is at most 10% apart (decision recorded in the plan).</p>
 *
 * @since 1.0
 */
public final class SubscriptionSuggester {

    /** Largest relative difference between two charges of the same subscription. */
    public static final BigDecimal SIMILARITY = new BigDecimal("0.10");

    /**
     * A charge of the previous billing period.
     *
     * @param description original description
     * @param amount      amount
     * @param single      {@code true} if it had no installments
     */
    public record PreviousCharge(String description, Money amount, boolean single) {
    }

    private SubscriptionSuggester() {
    }

    /**
     * Tells whether a purchase looks like a subscription.
     *
     * @param description description of the purchase
     * @param amount      amount of the purchase
     * @param previous    charges of the same card in the previous billing period
     * @return {@code true} to suggest {@code *}
     */
    public static boolean looksLikeSubscription(String description, Money amount, List<PreviousCharge> previous) {
        return previous.stream().filter(PreviousCharge::single)
                .filter(p -> DescriptionNormalizer.normalize(p.description()).equals(DescriptionNormalizer.normalize(description))
                        || DescriptionNormalizer.sameStore(p.description(), description))
                .anyMatch(p -> similar(p.amount(), amount));
    }

    private static boolean similar(Money a, Money b) {
        if (a.isZero() || b.isZero()) {
            return a.equals(b);
        }
        BigDecimal difference = a.minus(b).abs().amount();
        return difference.compareTo(a.abs().amount().multiply(SIMILARITY)) <= 0;
    }
}

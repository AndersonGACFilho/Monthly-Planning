package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.util.List;
import java.util.Set;

/**
 * Subscriptions of a period (RF-37, IMP-65): card subscriptions {@code *} plus every expense in a category marked as
 * subscription, such as a streaming service paid by boleto; a line that is both counts once.
 *
 * @since 1.0
 */
public final class SubscriptionTotalsCalculator {

    private static final String SUBSCRIPTION = "*";

    private SubscriptionTotalsCalculator() {
    }

    /**
     * Sums the subscriptions.
     *
     * @param lines                  expense lines of the period
     * @param subscriptionCategories categories marked as subscription
     * @return total of the subscriptions
     */
    public static Money total(List<ExpenseLine> lines, Set<Long> subscriptionCategories) {
        return lines.stream()
                .filter(l -> isSubscription(l.origin().installment(), l.categoryId(), subscriptionCategories))
                .map(ExpenseLine::amount)
                .reduce(Money.ZERO, Money::plus);
    }

    /**
     * Tells whether a line is a subscription: a card subscription {@code *} or a line in a category marked as
     * subscription.
     *
     * @param installment            {@code n/N}, {@code -}, {@code *}, or {@code null} outside the card
     * @param categoryId             category, or {@code null}
     * @param subscriptionCategories categories marked as subscription
     * @return {@code true} for a subscription
     */
    public static boolean isSubscription(String installment, Long categoryId, Set<Long> subscriptionCategories) {
        return SUBSCRIPTION.equals(installment) || categoryId != null && subscriptionCategories.contains(categoryId);
    }
}

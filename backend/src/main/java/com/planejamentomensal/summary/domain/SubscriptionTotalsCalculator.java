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
                .filter(l -> SUBSCRIPTION.equals(l.origin().installment())
                        || l.categoryId() != null && subscriptionCategories.contains(l.categoryId()))
                .map(ExpenseLine::amount)
                .reduce(Money.ZERO, Money::plus);
    }
}

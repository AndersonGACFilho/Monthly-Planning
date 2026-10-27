package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;

/**
 * Card bill total split by kind of charge (RF-37).
 *
 * @param single        one-off purchases, refunds, IOF and fees
 * @param installments  installments
 * @param subscriptions subscriptions ({@code *})
 * @since 1.0
 */
public record CardBillBreakdown(Money single, Money installments, Money subscriptions) {

    /** Nothing charged. */
    public static final CardBillBreakdown EMPTY = new CardBillBreakdown(Money.ZERO, Money.ZERO, Money.ZERO);

    /**
     * Adds one entry by its purchase type.
     *
     * @param type   {@code SINGLE}, {@code INSTALLMENT} or {@code SUBSCRIPTION}
     * @param amount amount
     * @return the new breakdown
     */
    public CardBillBreakdown plus(String type, Money amount) {
        return switch (type) {
            case "INSTALLMENT" -> new CardBillBreakdown(single, installments.plus(amount), subscriptions);
            case "SUBSCRIPTION" -> new CardBillBreakdown(single, installments, subscriptions.plus(amount));
            default -> new CardBillBreakdown(single.plus(amount), installments, subscriptions);
        };
    }

    /**
     * Returns the bill total.
     *
     * @return single + installments + subscriptions
     */
    public Money total() {
        return single.plus(installments).plus(subscriptions);
    }
}

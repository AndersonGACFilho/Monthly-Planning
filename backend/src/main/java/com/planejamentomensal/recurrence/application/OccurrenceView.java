package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;

/**
 * Read model of an income or fixed expense of one period, for other modules (SOL-06).
 *
 * @param id            occurrence
 * @param ruleId        rule
 * @param income        {@code true} for an income, {@code false} for a fixed expense
 * @param description   description
 * @param memberId      member
 * @param categoryId    category, or {@code null}
 * @param paymentMethod payment method of a fixed expense, or {@code null}
 * @param billingPeriod period
 * @param amount        amount of the period
 * @param done          received or paid
 * @param linkedEntryId card entry holding a card-paid expense, or {@code null}
 * @param day           expected or due day of the rule
 * @since 1.0
 */
public record OccurrenceView(long id, long ruleId, boolean income, String description, long memberId, Long categoryId,
        PaymentMethod paymentMethod, BillingPeriod billingPeriod, Money amount, boolean done, Long linkedEntryId, int day) {

    /**
     * Builds the view of an occurrence.
     *
     * @param o occurrence
     * @return its view
     */
    static OccurrenceView of(Occurrence o) {
        return new OccurrenceView(o.id(), o.rule().id(), o.rule().kind() == RuleKind.INCOME,
                o.rule().description(), o.rule().memberId(), o.rule().categoryId(), o.rule().paymentMethod(),
                o.billingPeriod(), o.amount(), o.status() == OccurrenceStatus.DONE,
                o.linkedEntryId(), o.rule().day());
    }
}

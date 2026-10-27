package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Amounts already committed to future bills by projected installments (RF-14, RF-17).
 *
 * @since 1.0
 */
public final class CommittedInstallmentsCalculator {

    /** Periods shown in the "committed in the next months" block (RF-17). */
    public static final int NEXT_PERIODS = 3;

    /**
     * A projected installment.
     *
     * @param billingPeriod period of the installment
     * @param cardId        primary card
     * @param amount        amount
     */
    public record FutureInstallment(BillingPeriod billingPeriod, long cardId, Money amount) {
    }

    /**
     * Committed amount of one future period.
     *
     * @param billingPeriod period
     * @param amount        sum of projected installments
     */
    public record PeriodAmount(BillingPeriod billingPeriod, Money amount) {
    }

    private CommittedInstallmentsCalculator() {
    }

    /**
     * Sums the projected installments of each of the next three periods (RF-17).
     *
     * @param current      current period
     * @param installments projected installments
     * @return three amounts, nearest period first
     */
    public static List<PeriodAmount> nextPeriods(BillingPeriod current, List<FutureInstallment> installments) {
        return IntStream.rangeClosed(1, NEXT_PERIODS)
                .mapToObj(current::plusMonths)
                .map(p -> new PeriodAmount(p, installments.stream().filter(i -> i.billingPeriod().equals(p))
                        .map(FutureInstallment::amount).reduce(Money.ZERO, Money::plus)))
                .toList();
    }

    /**
     * Sums the projected installments after the current period, per card (RF-14).
     *
     * @param current      current period
     * @param installments projected installments
     * @return committed amount by primary card
     */
    public static Map<Long, Money> byCard(BillingPeriod current, List<FutureInstallment> installments) {
        Map<Long, Money> result = new HashMap<>();
        installments.stream().filter(i -> i.billingPeriod().compareTo(current) > 0)
                .forEach(i -> result.merge(i.cardId(), i.amount(), Money::plus));
        return result;
    }
}

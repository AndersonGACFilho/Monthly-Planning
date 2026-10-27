package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.ArrayList;
import java.util.List;

/**
 * Plans the projected installments of a purchase (RN-04, RN-05, RN-09).
 *
 * <p>Registering installment {@code n/N} in period P projects {@code n+1 … N} in P+1 … P+N-n with the same
 * amount (RN-04). Earlier installments are never created backwards (RN-05). Subscriptions are projected in
 * every following period up to the horizon (RN-09).</p>
 *
 * @since 1.0
 */
public final class InstallmentProjector {

    /**
     * One projected installment.
     *
     * @param installmentNumber installment number, or {@code null} for a subscription charge
     * @param period            billing period it falls in
     */
    public record Projection(Integer installmentNumber, BillingPeriod period) {
    }

    private InstallmentProjector() {
    }

    /**
     * Plans the installments after {@code n/N} (RN-04).
     *
     * @param installmentNumber installment registered, {@code n}
     * @param installmentCount  installment count, {@code N}
     * @param period            billing period of installment {@code n}
     * @return installments {@code n+1 … N}, one per following period
     */
    public static List<Projection> remainingInstallments(int installmentNumber, int installmentCount,
            BillingPeriod period) {
        List<Projection> projections = new ArrayList<>();
        for (int next = installmentNumber + 1; next <= installmentCount; next++) {
            projections.add(new Projection(next, period.plusMonths(next - (long) installmentNumber)));
        }
        return projections;
    }

    /**
     * Plans the subscription charges after a period, up to a horizon (RN-09).
     *
     * @param lastPeriod    period of the last known charge
     * @param horizonEnd    last period to project, inclusive
     * @return one charge per period after {@code lastPeriod} up to {@code horizonEnd}
     */
    public static List<Projection> subscriptionCharges(BillingPeriod lastPeriod, BillingPeriod horizonEnd) {
        List<Projection> projections = new ArrayList<>();
        for (BillingPeriod p = lastPeriod.next(); p.compareTo(horizonEnd) <= 0; p = p.next()) {
            projections.add(new Projection(null, p));
        }
        return projections;
    }
}

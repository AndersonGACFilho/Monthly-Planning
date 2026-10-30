package com.planejamentomensal.carryover.application;

import com.planejamentomensal.carryover.domain.CarryOver;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;

/**
 * Read model of a carry-over, for other modules (SOL-06).
 *
 * @param memberId   member who owes
 * @param fromPeriod period the amount comes from
 * @param toPeriod   period the amount is owed in
 * @param amount     positive amount
 * @since 1.0
 */
public record CarryOverView(long memberId, BillingPeriod fromPeriod, BillingPeriod toPeriod, Money amount) {

    /**
     * Builds the view of a carry-over.
     *
     * @param c carry-over
     * @return its view
     */
    static CarryOverView of(CarryOver c) {
        return new CarryOverView(c.memberId(), c.fromPeriod(), c.toPeriod(), c.amount());
    }
}

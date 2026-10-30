package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.util.Map;
import java.util.Set;

/**
 * Amounts carried into and out of a period by member (RF-28). They change what a third party owes and reimburses,
 * never the expense of the period.
 *
 * @param incoming amount owed from the previous period, by member
 * @param outgoing amount carried into the next period, by member
 * @since 1.0
 */
public record CarryOvers(Map<Long, Money> incoming, Map<Long, Money> outgoing) {

    /** No carry-over at all. */
    public static final CarryOvers NONE = new CarryOvers(Map.of(), Map.of());

    /**
     * Copies the maps.
     *
     * @param incoming amount owed from the previous period, by member
     * @param outgoing amount carried into the next period, by member
     */
    public CarryOvers {
        incoming = Map.copyOf(incoming);
        outgoing = Map.copyOf(outgoing);
    }

    /**
     * Returns what a member owes from the previous period.
     *
     * @param memberId member
     * @return amount, zero if none
     */
    public Money in(long memberId) {
        return incoming.getOrDefault(memberId, Money.ZERO);
    }

    /**
     * Returns what a member carried into the next period.
     *
     * @param memberId member
     * @return amount, zero if none
     */
    public Money out(long memberId) {
        return outgoing.getOrDefault(memberId, Money.ZERO);
    }

    /**
     * Net change of the reimbursements of some members: carried in minus carried out.
     *
     * @param memberIds members
     * @return net amount
     */
    public Money net(Set<Long> memberIds) {
        return memberIds.stream().map(id -> in(id).minus(out(id))).reduce(Money.ZERO, Money::plus);
    }
}

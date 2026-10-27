package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * "Who owes whom": a member owes the payer of every expense assigned to them, netted per pair (RF-27).
 *
 * @since 1.0
 */
public final class SettlementCalculator {

    /**
     * One debt.
     *
     * @param fromMemberId member who owes
     * @param toMemberId   member who paid
     * @param amount       positive amount
     */
    public record Settlement(long fromMemberId, long toMemberId, Money amount) {
    }

    private SettlementCalculator() {
    }

    /**
     * Nets the debts between each pair of members.
     *
     * @param lines expense lines with their payer
     * @return debts with a positive amount, largest first
     */
    public static List<Settlement> calculate(List<ExpenseLine> lines) {
        Map<String, Money> byPair = new TreeMap<>();
        for (ExpenseLine line : lines) {
            if (line.memberId() == line.payerId()) {
                continue;
            }
            long low = Math.min(line.memberId(), line.payerId());
            long high = Math.max(line.memberId(), line.payerId());
            Money owedByLowToHigh = line.memberId() == low ? line.amount() : line.amount().negate();
            byPair.merge(low + ":" + high, owedByLowToHigh, Money::plus);
        }
        List<Settlement> result = new ArrayList<>();
        byPair.forEach((pair, amount) -> {
            long low = Long.parseLong(pair.substring(0, pair.indexOf(':')));
            long high = Long.parseLong(pair.substring(pair.indexOf(':') + 1));
            if (amount.amount().signum() > 0) {
                result.add(new Settlement(low, high, amount));
            } else if (amount.amount().signum() < 0) {
                result.add(new Settlement(high, low, amount.negate()));
            }
        });
        result.sort(Comparator.comparing((Settlement s) -> s.amount().amount()).reversed());
        return result;
    }
}

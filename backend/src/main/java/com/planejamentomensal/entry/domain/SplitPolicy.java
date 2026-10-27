package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Divides an amount between members without losing cents (RF-26).
 *
 * <p>Splits by amount take their fixed share first; the rest is divided by the percentages. A purchase
 * belongs 100% to one member by default.</p>
 *
 * @since 1.0
 */
public final class SplitPolicy {

    /**
     * Share of one member.
     *
     * @param memberId member
     * @param amount   share
     */
    public record Share(long memberId, Money amount) {
    }

    private SplitPolicy() {
    }

    /**
     * Divides an amount.
     *
     * @param total  amount to divide (may be negative for refunds)
     * @param splits member shares by percentage or amount
     * @return one share per split, summing exactly to {@code total}
     * @throws IllegalArgumentException if there are no splits
     */
    public static List<Share> divide(Money total, List<PurchaseSplit> splits) {
        if (splits.isEmpty()) {
            throw new IllegalArgumentException("No members to split between");
        }
        List<Share> shares = new ArrayList<>();
        Money fixed = Money.ZERO;
        List<PurchaseSplit> byPercentage = new ArrayList<>();
        for (PurchaseSplit split : splits) {
            if (split.amount() != null) {
                Money share = total.isNegative() ? split.amount().abs().negate() : split.amount();
                shares.add(new Share(split.memberId(), share));
                fixed = fixed.plus(share);
            } else {
                byPercentage.add(split);
            }
        }
        Money rest = total.minus(fixed);
        if (!byPercentage.isEmpty()) {
            List<BigDecimal> weights = byPercentage.stream().map(PurchaseSplit::percentage).toList();
            List<Money> parts = weights.stream().allMatch(w -> w.signum() == 0)
                    ? rest.allocate(byPercentage.stream().map(s -> BigDecimal.ONE).toList())
                    : rest.allocate(weights);
            for (int i = 0; i < byPercentage.size(); i++) {
                shares.add(new Share(byPercentage.get(i).memberId(), parts.get(i)));
            }
        } else if (!rest.isZero()) {
            Share last = shares.removeLast();
            shares.add(new Share(last.memberId(), last.amount().plus(rest)));
        }
        return shares;
    }

    /**
     * Tells whether splits assign the whole amount (RF-76): fixed amounts add up to the installment
     * amount, or percentages add up to 100 over what the fixed amounts leave.
     *
     * @param installmentAmount amount of one installment
     * @param splits            member shares
     * @return {@code true} if the whole amount is assigned
     */
    public static boolean isComplete(Money installmentAmount, List<PurchaseSplit> splits) {
        if (splits.isEmpty()) {
            return false;
        }
        Money total = installmentAmount.abs();
        Money fixed = splits.stream().filter(s -> s.amount() != null).map(PurchaseSplit::amount).reduce(Money.ZERO, Money::plus);
        List<BigDecimal> percentages = splits.stream().filter(s -> s.percentage() != null).map(PurchaseSplit::percentage).toList();
        if (percentages.isEmpty()) {
            return fixed.equals(total);
        }
        BigDecimal percent = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return percent.compareTo(BigDecimal.valueOf(100)) == 0 && fixed.compareTo(total) <= 0;
    }
}

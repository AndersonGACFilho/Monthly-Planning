package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Expense by category with its share of the total and the change from the previous month (RF-12).
 *
 * @since 1.0
 */
public final class CategoryTotalsCalculator {

    /**
     * Total of one category.
     *
     * @param categoryId     category, or {@code null} for "not classified"
     * @param amount         amount in the period
     * @param percentOfTotal share of the total expense
     * @param previousAmount amount in the previous period
     * @param variation      change from the previous period in percent, or {@code null} if it had nothing
     */
    public record CategoryTotal(Long categoryId, Money amount, BigDecimal percentOfTotal, Money previousAmount,
            BigDecimal variation) {
    }

    private CategoryTotalsCalculator() {
    }

    /**
     * Calculates the totals, largest first.
     *
     * @param current  expense lines of the period
     * @param previous expense lines of the previous period
     * @return one total per category present in either period
     */
    public static List<CategoryTotal> calculate(List<ExpenseLine> current, List<ExpenseLine> previous) {
        Map<Long, Money> now = sum(current);
        Map<Long, Money> before = sum(previous);
        Money total = now.values().stream().reduce(Money.ZERO, Money::plus);
        Set<Long> categories = new HashSet<>(now.keySet());
        categories.addAll(before.keySet());
        return categories.stream()
                .map(id -> {
                    Money amount = now.getOrDefault(id, Money.ZERO);
                    Money last = before.getOrDefault(id, Money.ZERO);
                    return new CategoryTotal(id, amount, Percentages.of(amount, total), last, Percentages.variation(amount, last));
                })
                .sorted(Comparator.comparing((CategoryTotal t) -> t.amount().amount()).reversed()
                        .thenComparing(t -> Objects.requireNonNullElse(t.categoryId(), Long.MAX_VALUE)))
                .toList();
    }

    private static Map<Long, Money> sum(List<ExpenseLine> lines) {
        Map<Long, Money> result = new HashMap<>();
        lines.forEach(l -> result.merge(l.categoryId(), l.amount(), Money::plus));
        return result;
    }
}

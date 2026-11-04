package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Every item behind the total of one category: fixed and variable expenses as well as card entries (RF-12, RF-38).
 * The member shares of one record become a single item, so the items add up to the category total.
 *
 * @since 1.0
 */
public final class CategoryItemsCalculator {

    /**
     * One record counted in a category.
     *
     * @param source      fixed, variable or card
     * @param referenceId occurrence, variable expense or card entry
     * @param description description shown to the user
     * @param date        due day, expense date or entry date
     * @param installment installment of a card entry, otherwise {@code null}
     * @param cardId      primary card of a card entry, otherwise {@code null}
     * @param amount      sum of the shares that count
     * @param memberIds   members who bear the amount, in the order they appear
     */
    public record CategoryItem(ExpenseSource source, long referenceId, String description, LocalDate date,
            String installment, Long cardId, Money amount, List<Long> memberIds) {
    }

    private record Key(ExpenseSource source, long referenceId) {
    }

    private CategoryItemsCalculator() {
    }

    /**
     * Lists the items of a category, by source (fixed, variable, card) and then by date.
     *
     * @param lines      expense lines that count, member filter already applied
     * @param categoryId category, or {@code null} for "not classified"
     * @return the items
     */
    public static List<CategoryItem> calculate(List<ExpenseLine> lines, Long categoryId) {
        Map<Key, List<ExpenseLine>> byOrigin = new LinkedHashMap<>();
        lines.stream().filter(l -> Objects.equals(l.categoryId(), categoryId))
                .forEach(l -> byOrigin.computeIfAbsent(new Key(l.source(), l.origin().referenceId()), k -> new ArrayList<>())
                        .add(l));
        return byOrigin.values().stream().map(CategoryItemsCalculator::item)
                .sorted(Comparator.comparing(CategoryItem::source)
                        .thenComparing(CategoryItem::date, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CategoryItem::description, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static CategoryItem item(List<ExpenseLine> shares) {
        ExpenseLine first = shares.getFirst();
        ExpenseOrigin origin = first.origin();
        Money amount = shares.stream().map(ExpenseLine::amount).reduce(Money.ZERO, Money::plus);
        List<Long> members = shares.stream().map(ExpenseLine::memberId).distinct().toList();
        return new CategoryItem(first.source(), origin.referenceId(), origin.description(), origin.date(),
                origin.installment(), first.cardId(), amount, members);
    }
}

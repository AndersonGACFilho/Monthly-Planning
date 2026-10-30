package com.planejamentomensal.summary.application;

import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.category.application.CategoryView;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CarryOvers;
import com.planejamentomensal.summary.domain.ExpenseLine;
import com.planejamentomensal.summary.domain.HouseholdTotals;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Month-by-month totals of a range of billing periods for the history tab, by the same rules as the overview
 * ({@link HouseholdTotals}), optionally for one member and one category (with its subcategories).
 *
 * @since 1.0
 */
@Service
@Transactional
public class HistoryService {

    /** Longest range, in months. */
    public static final int MAX_MONTHS = 120;

    private final PeriodData data;
    private final MemberQueries members;
    private final CategoryQueries categories;

    /**
     * Creates the service.
     *
     * @param data       period data
     * @param members    member queries
     * @param categories category queries
     */
    public HistoryService(PeriodData data, MemberQueries members, CategoryQueries categories) {
        this.data = data;
        this.members = members;
        this.categories = categories;
    }

    /**
     * Builds the history.
     *
     * @param from       first period
     * @param to         last period
     * @param memberId   member filter, or {@code null}
     * @param categoryId category filter (subcategories included), or {@code null}
     * @return one point per month, oldest first
     * @throws InvalidInputException with code {@code history-range-invalid} for an empty or too long range
     */
    public List<HistoryPoint> history(BillingPeriod from, BillingPeriod to, Long memberId, Long categoryId) {
        long months = from.monthsUntil(to);
        if (months < 0 || months >= MAX_MONTHS) {
            throw new InvalidInputException("history-range-invalid", from, to, MAX_MONTHS);
        }
        Set<Long> thirdParties = members.all().stream().filter(m -> !m.main()).map(MemberView::id).collect(Collectors.toSet());
        Predicate<ExpenseLine> inCategory = categoryFilter(categoryId);
        List<HistoryPoint> points = new ArrayList<>();
        for (BillingPeriod period = from; period.compareTo(to) <= 0; period = period.next()) {
            PeriodData.Lines lines = data.collect(period);
            List<ExpenseLine> expenses = lines.expenses().stream().filter(inCategory).toList();
            CarryOvers carries = categoryId == null ? lines.carries() : CarryOvers.NONE;
            HouseholdTotals totals = HouseholdTotals.of(lines.incomes(), expenses, thirdParties, memberId, carries);
            Map<Long, Money> byCategory = new LinkedHashMap<>();
            HouseholdTotals.of(lines.incomes(), lines.expenses(), thirdParties, memberId, carries).lines()
                    .forEach(l -> byCategory.merge(l.categoryId(), l.amount(), Money::plus));
            Map<Long, Money> byMember = new LinkedHashMap<>();
            expenses.forEach(l -> byMember.merge(l.memberId(), l.amount(), Money::plus));
            points.add(new HistoryPoint(period, totals.income(), totals.mainIncome(), totals.reimbursements(),
                    totals.expense(), totals.fixed(), totals.variable(), totals.card(), byCategory, byMember));
        }
        return points;
    }

    private Predicate<ExpenseLine> categoryFilter(Long categoryId) {
        if (categoryId == null) {
            return l -> true;
        }
        List<CategoryView> all = categories.all();
        Set<Long> ids = new HashSet<>();
        ids.add(categoryId);
        boolean added = true;
        while (added) {
            added = all.stream().filter(c -> c.parentId() != null && ids.contains(c.parentId())).map(CategoryView::id)
                    .map(ids::add).reduce(false, Boolean::logicalOr);
        }
        boolean unclassified = all.stream().anyMatch(c -> c.system() && Objects.equals(c.id(), categoryId));
        return l -> l.categoryId() == null ? unclassified : ids.contains(l.categoryId());
    }
}

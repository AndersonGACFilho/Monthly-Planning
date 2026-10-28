package com.planejamentomensal.summary.api;

import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.application.HistoryPoint;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.hateoas.RepresentationModel;

/**
 * Month-by-month history of incomes and expenses for the history tab.
 *
 * @since 1.0
 */
public class HistoryModel extends RepresentationModel<HistoryModel> {

    /**
     * One month.
     *
     * @param billingPeriod  period, {@code yyyy-MM}
     * @param income         income
     * @param mainIncome     incomes of MAIN members (or of the filtered member)
     * @param reimbursements third-party share of the expenses
     * @param expense        expense in the filters
     * @param fixed          fixed expenses not paid by card
     * @param variable       variable expenses
     * @param card           card shares
     * @param balance        income − expense
     * @param byCategory     expense by category id ({@code none} for not classified)
     * @param byMember       expense by member id
     */
    public record HistoryMonth(String billingPeriod, BigDecimal income, BigDecimal mainIncome, BigDecimal reimbursements,
            BigDecimal expense, BigDecimal fixed, BigDecimal variable, BigDecimal card, BigDecimal balance,
            Map<String, BigDecimal> byCategory, Map<String, BigDecimal> byMember) {

        static HistoryMonth of(HistoryPoint p) {
            return new HistoryMonth(p.period().toString(), p.income().amount(), p.mainIncome().amount(), p.reimbursements().amount(),
                    p.expense().amount(), p.fixed().amount(), p.variable().amount(), p.card().amount(), p.balance().amount(),
                    keyed(p.byCategory()), keyed(p.byMember()));
        }

        private static Map<String, BigDecimal> keyed(Map<Long, Money> values) {
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            values.forEach((id, amount) -> result.put(id == null ? "none" : String.valueOf(id), amount.amount()));
            return result;
        }
    }

    /** First period. */
    public final String from;
    /** Last period. */
    public final String to;
    /** Member filter, or {@code null}. */
    public final Long memberId;
    /** Category filter, or {@code null}. */
    public final Long categoryId;
    /** Months, oldest first. */
    public final List<HistoryMonth> points;

    /**
     * Creates the model.
     *
     * @param from       first period
     * @param to         last period
     * @param memberId   member filter
     * @param categoryId category filter
     * @param points     months
     */
    public HistoryModel(String from, String to, Long memberId, Long categoryId, List<HistoryPoint> points) {
        this.from = from;
        this.to = to;
        this.memberId = memberId;
        this.categoryId = categoryId;
        this.points = points.stream().map(HistoryMonth::of).toList();
    }
}

package com.planejamentomensal.summary.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.util.Map;

/**
 * Totals of one month of the history, with the member and category filters applied.
 *
 * @param period         billing period
 * @param income         income (MAIN incomes + reimbursements, or the filtered member's incomes)
 * @param mainIncome     incomes of MAIN members, or of the filtered member
 * @param reimbursements third-party share of the expenses
 * @param expense        expense in the filters
 * @param fixed          fixed expenses not paid by card, in the filters
 * @param variable       variable expenses, in the filters
 * @param card           card shares, in the filters
 * @param byCategory     expense by category id ({@code null} for not classified), member filter applied
 * @param byMember       expense by member id, category filter applied
 * @since 1.0
 */
public record HistoryPoint(BillingPeriod period, Money income, Money mainIncome, Money reimbursements, Money expense,
        Money fixed, Money variable, Money card, Map<Long, Money> byCategory, Map<Long, Money> byMember) {

    /**
     * Returns the balance.
     *
     * @return income − expense
     */
    public Money balance() {
        return income.minus(expense);
    }
}

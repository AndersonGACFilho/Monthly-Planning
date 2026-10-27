package com.planejamentomensal.summary.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CategoryTotalsCalculator.CategoryTotal;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator.PeriodAmount;
import com.planejamentomensal.summary.domain.Indicators;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator.MemberTotal;
import com.planejamentomensal.summary.domain.SettlementCalculator.Settlement;
import java.util.List;

/**
 * Overview of a billing period (RF-10 to RF-17, RF-25, RF-27, RF-79a).
 *
 * @param billingPeriod     period
 * @param memberId          member filter (RF-15), or {@code null} for everyone
 * @param indicators        income, expense, balance, % spent and alert (RF-10, RF-16)
 * @param mainIncome        incomes of MAIN members (or of the filtered member)
 * @param reimbursements    third-party share of the expenses, counted as income (XLS-04, Q8)
 * @param fixedTotal        fixed expenses not paid by card
 * @param variableTotal     variable expenses
 * @param cardTotal         card bills (RF-11)
 * @param categories        expense by category (RF-12)
 * @param members           totals by member (RF-13, RF-25)
 * @param cards             totals by card (RF-14, RF-36, RF-37)
 * @param committedNext     projected installments of the next three periods (RF-17)
 * @param settlements       who owes whom (RF-27)
 * @param pendingCount      card entries still pending review (RF-79a)
 * @param pendingAmount     their sum
 * @param unconfirmedCount  UNCONFIRMED entries left out of the totals (Q6)
 * @since 1.0
 */
public record MonthlySummary(BillingPeriod billingPeriod, Long memberId, Indicators indicators, Money mainIncome,
        Money reimbursements, Money fixedTotal, Money variableTotal, Money cardTotal, List<CategoryTotal> categories,
        List<MemberTotal> members, List<CardBillSummary> cards, List<PeriodAmount> committedNext,
        List<Settlement> settlements, int pendingCount, Money pendingAmount, int unconfirmedCount) {
}

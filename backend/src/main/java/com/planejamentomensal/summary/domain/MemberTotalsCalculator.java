package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.util.List;

/**
 * Income, expense by source and balance of each member (RF-13, RF-25).
 *
 * @since 1.0
 */
public final class MemberTotalsCalculator {

    /**
     * Totals of one member.
     *
     * @param memberId     member
     * @param main         {@code true} for a MAIN member
     * @param income       incomes of the member
     * @param fixed        fixed expenses not paid by card
     * @param variable     variable expenses
     * @param card         share of card entries
     * @param expense      fixed + variable + card
     * @param balance      income − expense
     * @param percentSpent expense ÷ income × 100, or {@code null} without income
     */
    public record MemberTotal(long memberId, boolean main, Money income, Money fixed, Money variable, Money card,
            Money expense, Money balance, BigDecimal percentSpent) {
    }

    private MemberTotalsCalculator() {
    }

    /**
     * Calculates the totals of every member, in the given order.
     *
     * @param members  members
     * @param incomes  income lines
     * @param expenses expense lines
     * @return one total per member
     */
    public static List<MemberTotal> calculate(List<MemberKindInfo> members, List<IncomeLine> incomes,
            List<ExpenseLine> expenses) {
        return members.stream().map(m -> {
            Money income = incomes.stream().filter(i -> i.memberId() == m.id()).map(IncomeLine::amount)
                    .reduce(Money.ZERO, Money::plus);
            Money fixed = sum(expenses, m.id(), ExpenseSource.FIXED);
            Money variable = sum(expenses, m.id(), ExpenseSource.VARIABLE);
            Money card = sum(expenses, m.id(), ExpenseSource.CARD);
            Money expense = fixed.plus(variable).plus(card);
            return new MemberTotal(m.id(), m.main(), income, fixed, variable, card, expense, income.minus(expense),
                    Percentages.of(expense, income));
        }).toList();
    }

    private static Money sum(List<ExpenseLine> lines, long memberId, ExpenseSource source) {
        return lines.stream().filter(l -> l.memberId() == memberId && l.source() == source).map(ExpenseLine::amount)
                .reduce(Money.ZERO, Money::plus);
    }
}

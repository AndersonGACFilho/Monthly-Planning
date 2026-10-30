package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.util.List;
import java.util.Set;

/**
 * Income and expense totals of a period for the household or for one member (RF-10, RF-11, RF-15, RF-28, XLS-04,
 * Q8): household income = incomes of MAIN members + the third-party share of the expenses (reimbursements), plus what
 * third parties carried in from the previous period and minus what they carried out to the next; expense = fixed (not
 * card) + variable + card shares. For one member only that member's incomes and shares count.
 *
 * @param lines          expense lines that count
 * @param mainIncome     incomes of MAIN members, or of the filtered member
 * @param reimbursements third-party share of the expenses adjusted by their carry-overs, zero with a member filter
 * @param fixed          fixed expenses not paid by card
 * @param variable       variable expenses
 * @param card           card shares
 * @since 1.0
 */
public record HouseholdTotals(List<ExpenseLine> lines, Money mainIncome, Money reimbursements, Money fixed, Money variable,
        Money card) {

    /**
     * Calculates the totals.
     *
     * @param incomes      income lines of the period
     * @param expenses     expense lines of the period
     * @param thirdParties ids of third-party members
     * @param memberId     member filter, or {@code null} for the household
     * @param carries      amounts carried in and out of the period
     * @return totals
     */
    public static HouseholdTotals of(List<IncomeLine> incomes, List<ExpenseLine> expenses, Set<Long> thirdParties,
            Long memberId, CarryOvers carries) {
        List<ExpenseLine> lines = expenses.stream().filter(l -> memberId == null || l.memberId() == memberId).toList();
        Money income = sum(incomes.stream()
                .filter(i -> memberId == null ? !thirdParties.contains(i.memberId()) : i.memberId() == memberId)
                .map(IncomeLine::amount).toList());
        Money reimbursements = memberId != null ? Money.ZERO
                : sum(lines.stream().filter(l -> thirdParties.contains(l.memberId())).map(ExpenseLine::amount).toList())
                        .plus(carries.net(thirdParties));
        return new HouseholdTotals(lines, income, reimbursements, sum(lines, ExpenseSource.FIXED),
                sum(lines, ExpenseSource.VARIABLE), sum(lines, ExpenseSource.CARD));
    }

    /**
     * Returns the total income.
     *
     * @return main income + reimbursements
     */
    public Money income() {
        return mainIncome.plus(reimbursements);
    }

    /**
     * Returns the total expense.
     *
     * @return fixed + variable + card
     */
    public Money expense() {
        return fixed.plus(variable).plus(card);
    }

    private static Money sum(List<ExpenseLine> lines, ExpenseSource source) {
        return sum(lines.stream().filter(l -> l.source() == source).map(ExpenseLine::amount).toList());
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money.ZERO, Money::plus);
    }
}

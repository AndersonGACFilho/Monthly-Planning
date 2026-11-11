package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/**
 * Statement of one member in one billing period (RF-82): what the member owes, line by line.
 *
 * @param memberId        member
 * @param memberCode      member code
 * @param memberName      member name
 * @param main            {@code true} for a MAIN member (income and balance apply)
 * @param billingPeriod   period
 * @param issuedOn        issue date
 * @param cards           card entries grouped by card
 * @param fixedExpenses   fixed expenses of the member not paid by card
 * @param variableExpenses variable expenses of the member
 * @param future          expenses of the next periods: installments and fixed expenses (empty when not included)
 * @param income          incomes of the member
 * @param cardTotal       sum of the card groups
 * @param fixedTotal      sum of the fixed expenses
 * @param variableTotal   sum of the variable expenses
 * @param previousBalance amount still owed from the previous period, or {@code null} (RF-28)
 * @param carriedOut      amount carried into the next period, or {@code null} (RF-28)
 * @param total           total due: card + fixed + variable + previous balance
 * @param dueDate         earliest card due date of the period, or {@code null}
 * @param pixKey          PIX key for the payment, or {@code null}
 * @param pendingItems    descriptions of entries still pending review (RF-79b)
 * @param options         options used
 * @since 1.0
 */
public record MemberStatement(long memberId, String memberCode, String memberName, boolean main, BillingPeriod billingPeriod,
        LocalDate issuedOn, List<CardGroup> cards, List<ExpenseLine> fixedExpenses, List<ExpenseLine> variableExpenses,
        List<FuturePeriod> future, Money income, Money cardTotal, Money fixedTotal, Money variableTotal,
        Carry previousBalance, Carry carriedOut, Money total, LocalDate dueDate, String pixKey, List<String> pendingItems, StatementOptions options) {

    /**
     * Returns the balance: what the member earned or paid minus what they owe, after the carry-over.
     *
     * @return income − total + carried out
     */
    public Money balance() {
        return income.minus(total).plus(carriedOut == null ? Money.ZERO : carriedOut.amount());
    }

    /**
     * Returns the subscriptions of the period: card subscriptions {@code *} and expenses in subscription categories
     * (IMP-65).
     *
     * @return total of the subscriptions
     */
    public Money subscriptionTotal() {
        Money card = cards.stream().flatMap(g -> g.lines().stream()).filter(l -> "*".equals(l.installment()))
                .map(CardLine::amount).reduce(Money.ZERO, Money::plus);
        return Stream.concat(fixedExpenses.stream(), variableExpenses.stream())
                .filter(ExpenseLine::subscription).map(ExpenseLine::amount).reduce(card, Money::plus);
    }

    /**
     * Amount carried between this period and a neighbouring one (RF-28).
     *
     * @param billingPeriod the other period: where it came from, or where it went
     * @param amount        positive amount
     */
    public record Carry(BillingPeriod billingPeriod, Money amount) {
    }

    /**
     * Entries of one card.
     *
     * @param cardName   card nickname
     * @param lastDigits finals
     * @param dueDate    due date of the bill
     * @param lines      entries, by date
     * @param total      sum of the member shares
     */
    public record CardGroup(String cardName, List<String> lastDigits, LocalDate dueDate, List<CardLine> lines, Money total) {
    }

    /**
     * One card entry, with the member share.
     *
     * @param date        entry date
     * @param description displayed or original description (RF-83)
     * @param installment {@code n/N}, {@code -} or {@code *}
     * @param amount      member share
     * @param percent     share percentage when the entry is split, otherwise {@code null}
     * @param category    category name, or {@code null}
     * @param pending     {@code true} while pending review
     */
    public record CardLine(LocalDate date, String description, String installment, Money amount, BigDecimal percent,
            String category, boolean pending) {
    }

    /**
     * One fixed or variable expense.
     *
     * @param date        date, or {@code null} for fixed expenses
     * @param description description
     * @param amount       amount
     * @param category     category name, or {@code null}
     * @param subscription {@code true} when its category is marked as subscription (IMP-65)
     */
    public record ExpenseLine(LocalDate date, String description, Money amount, String category, boolean subscription) {
    }

    /**
     * What the member will owe in a future period, line by line (IMP-64).
     *
     * @param billingPeriod period
     * @param lines         installments, subscriptions and fixed expenses of the period
     * @param total         sum of the lines
     */
    public record FuturePeriod(BillingPeriod billingPeriod, List<FutureLine> lines, Money total) {
    }

    /**
     * One expense of a future period.
     *
     * @param description description shown
     * @param installment installment notation of a card line ({@code 02/10}, {@code *}), or {@code null} for a fixed
     *                    expense
     * @param amount      member share
     */
    public record FutureLine(String description, String installment, Money amount) {
    }
}

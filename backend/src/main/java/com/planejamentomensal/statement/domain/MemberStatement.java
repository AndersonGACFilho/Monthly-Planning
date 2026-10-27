package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
 * @param committed       installments committed in the next periods (empty when not included)
 * @param income          incomes of the member
 * @param cardTotal       sum of the card groups
 * @param fixedTotal      sum of the fixed expenses
 * @param variableTotal   sum of the variable expenses
 * @param total           total due: card + fixed + variable
 * @param dueDate         earliest card due date of the period, or {@code null}
 * @param pixKey          PIX key for the payment, or {@code null}
 * @param pendingItems    descriptions of entries still pending review (RF-79b)
 * @param options         options used
 * @since 1.0
 */
public record MemberStatement(long memberId, String memberCode, String memberName, boolean main, BillingPeriod billingPeriod,
        LocalDate issuedOn, List<CardGroup> cards, List<ExpenseLine> fixedExpenses, List<ExpenseLine> variableExpenses,
        List<CommittedPeriod> committed, Money income, Money cardTotal, Money fixedTotal, Money variableTotal, Money total,
        LocalDate dueDate, String pixKey, List<String> pendingItems, StatementOptions options) {

    /**
     * Returns the balance of a MAIN member.
     *
     * @return income − total
     */
    public Money balance() {
        return income.minus(total);
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
     * @param amount      amount
     * @param category    category name, or {@code null}
     */
    public record ExpenseLine(LocalDate date, String description, Money amount, String category) {
    }

    /**
     * Installments of the member committed in a future period.
     *
     * @param billingPeriod period
     * @param amount        member share of the projected installments
     */
    public record CommittedPeriod(BillingPeriod billingPeriod, Money amount) {
    }
}

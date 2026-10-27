package com.planejamentomensal.summary.api;

import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.application.MonthlySummary;
import com.planejamentomensal.summary.domain.CategoryTotalsCalculator.CategoryTotal;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator.MemberTotal;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

/**
 * HAL representation of the overview of a billing period (RF-10 to RF-17, RF-25, RF-27, RF-79a).
 *
 * @since 1.0
 */
public class MonthlySummaryModel extends RepresentationModel<MonthlySummaryModel> {

    /**
     * Expense of one category (RF-12).
     *
     * @param categoryId     category, or {@code null} for "not classified"
     * @param amount         amount
     * @param percentOfTotal share of the total expense
     * @param previousAmount amount in the previous period
     * @param variation      change from the previous period in percent, or {@code null}
     */
    public record CategoryRow(Long categoryId, BigDecimal amount, BigDecimal percentOfTotal, BigDecimal previousAmount,
            BigDecimal variation) {

        static CategoryRow of(CategoryTotal t) {
            return new CategoryRow(t.categoryId(), t.amount().amount(), t.percentOfTotal(), t.previousAmount().amount(),
                    t.variation());
        }
    }

    /**
     * Totals of one member (RF-13, RF-25).
     *
     * @param memberId     member
     * @param main         {@code true} for a MAIN member
     * @param income       incomes
     * @param fixed        fixed expenses not paid by card
     * @param variable     variable expenses
     * @param card         share of card entries
     * @param expense      total expense
     * @param balance      income − expense
     * @param percentSpent expense ÷ income × 100, or {@code null}
     */
    public record MemberRow(long memberId, boolean main, BigDecimal income, BigDecimal fixed, BigDecimal variable,
            BigDecimal card, BigDecimal expense, BigDecimal balance, BigDecimal percentSpent) {

        static MemberRow of(MemberTotal t) {
            return new MemberRow(t.memberId(), t.main(), t.income().amount(), t.fixed().amount(), t.variable().amount(),
                    t.card().amount(), t.expense().amount(), t.balance().amount(), t.percentSpent());
        }
    }

    /**
     * Committed amount of a future period (RF-17).
     *
     * @param billingPeriod period
     * @param amount        projected installments
     */
    public record CommittedRow(String billingPeriod, BigDecimal amount) {
    }

    /**
     * One debt between members (RF-27).
     *
     * @param fromMemberId member who owes
     * @param toMemberId   member who paid
     * @param amount       amount
     */
    public record SettlementRow(long fromMemberId, long toMemberId, BigDecimal amount) {
    }

    /** Billing period. */
    public final String billingPeriod;
    /** Member filter, or {@code null}. */
    public final Long memberId;
    /** Total income: MAIN incomes + reimbursements. */
    public final BigDecimal income;
    /** Total expense. */
    public final BigDecimal expense;
    /** Income − expense. */
    public final BigDecimal balance;
    /** Expense ÷ income × 100, or {@code null}. */
    public final BigDecimal percentSpent;
    /** {@code NONE}, {@code WARNING} or {@code DANGER} (RF-16). */
    public final String alert;
    /** Incomes of MAIN members (or of the filtered member). */
    public final BigDecimal mainIncome;
    /** Third-party share of the expenses, counted as income (XLS-04). */
    public final BigDecimal reimbursements;
    /** Fixed expenses not paid by card. */
    public final BigDecimal fixedTotal;
    /** Variable expenses. */
    public final BigDecimal variableTotal;
    /** Card bills. */
    public final BigDecimal cardTotal;
    /** Expense by category, largest first. */
    public final List<CategoryRow> categories;
    /** Totals by member. */
    public final List<MemberRow> members;
    /** Totals by card. */
    public final List<CardBillModel> cards;
    /** Projected installments of the next three periods. */
    public final List<CommittedRow> committedNext;
    /** Who owes whom. */
    public final List<SettlementRow> settlements;
    /** Card entries pending review (RF-79a). */
    public final int pendingCount;
    /** Their sum. */
    public final BigDecimal pendingAmount;
    /** UNCONFIRMED entries left out of the totals. */
    public final int unconfirmedCount;

    /**
     * Copies an overview.
     *
     * @param s     overview
     * @param cards card representations with links
     */
    public MonthlySummaryModel(MonthlySummary s, List<CardBillModel> cards) {
        this.billingPeriod = s.billingPeriod().toString();
        this.memberId = s.memberId();
        this.income = s.indicators().income().amount();
        this.expense = s.indicators().expense().amount();
        this.balance = s.indicators().balance().amount();
        this.percentSpent = s.indicators().percentSpent();
        this.alert = s.indicators().alert().name();
        this.mainIncome = s.mainIncome().amount();
        this.reimbursements = s.reimbursements().amount();
        this.fixedTotal = s.fixedTotal().amount();
        this.variableTotal = s.variableTotal().amount();
        this.cardTotal = s.cardTotal().amount();
        this.categories = s.categories().stream().map(CategoryRow::of).toList();
        this.members = s.members().stream().map(MemberRow::of).toList();
        this.cards = cards;
        this.committedNext = s.committedNext().stream()
                .map(c -> new CommittedRow(c.billingPeriod().toString(), c.amount().amount())).toList();
        this.settlements = s.settlements().stream()
                .map(t -> new SettlementRow(t.fromMemberId(), t.toMemberId(), t.amount().amount())).toList();
        this.pendingCount = s.pendingCount();
        this.pendingAmount = amount(s.pendingAmount());
        this.unconfirmedCount = s.unconfirmedCount();
    }

    private static BigDecimal amount(Money money) {
        return money.amount();
    }
}

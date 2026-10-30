package com.planejamentomensal.summary.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CategoryTotalsCalculator.CategoryTotal;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator.FutureInstallment;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator.MemberTotal;
import com.planejamentomensal.summary.domain.SettlementCalculator.Settlement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SummaryCalculatorsTest {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    private static Money m(String amount) {
        return Money.of(amount);
    }

    private static ExpenseLine line(ExpenseSource source, long member, Long category, String amount, long payer) {
        return new ExpenseLine(source, member, category, m(amount), source == ExpenseSource.CARD ? 9L : null, payer);
    }

    @Test
    @DisplayName("RF-10, RF-16: indicators with balance, percent spent and alert thresholds")
    void indicators() {
        Indicators ok = Indicators.of(m("10000.00"), m("7000.00"), 80, 100);
        assertThat(ok.balance()).isEqualTo(m("3000.00"));
        assertThat(ok.percentSpent()).isEqualByComparingTo("70.0");
        assertThat(ok.alert()).isEqualTo(Indicators.AlertLevel.NONE);
        assertThat(Indicators.of(m("10000.00"), m("8000.00"), 80, 100).alert()).isEqualTo(Indicators.AlertLevel.WARNING);
        assertThat(Indicators.of(m("10000.00"), m("10500.00"), 80, 100).alert()).isEqualTo(Indicators.AlertLevel.DANGER);
        Indicators noIncome = Indicators.of(Money.ZERO, m("1.00"), 80, 100);
        assertThat(noIncome.percentSpent()).isNull();
        assertThat(noIncome.alert()).isEqualTo(Indicators.AlertLevel.DANGER);
        assertThat(Indicators.of(Money.ZERO, Money.ZERO, 80, 100).alert()).isEqualTo(Indicators.AlertLevel.NONE);
    }

    @Test
    @DisplayName("RF-12: category totals with share of total and variation from the previous month")
    void categories() {
        List<CategoryTotal> totals = CategoryTotalsCalculator.calculate(
                List.of(line(ExpenseSource.FIXED, 1, 1L, "600.00", 1), line(ExpenseSource.CARD, 1, 1L, "150.00", 1),
                        line(ExpenseSource.VARIABLE, 1, null, "250.00", 1)),
                List.of(line(ExpenseSource.FIXED, 1, 1L, "500.00", 1), line(ExpenseSource.FIXED, 1, 2L, "80.00", 1)));
        assertThat(totals).extracting(CategoryTotal::categoryId).containsExactly(1L, null, 2L);
        assertThat(totals.getFirst().amount()).isEqualTo(m("750.00"));
        assertThat(totals.getFirst().percentOfTotal()).isEqualByComparingTo("75.0");
        assertThat(totals.getFirst().variation()).isEqualByComparingTo("50.0");
        assertThat(totals.get(1).variation()).isNull();
        assertThat(totals.get(2).amount()).isEqualTo(Money.ZERO);
        assertThat(totals.get(2).variation()).isEqualByComparingTo("-100.0");
    }

    @Test
    @DisplayName("RF-13, RF-25: member totals split expense into fixed, variable and card")
    void members() {
        List<MemberTotal> totals = MemberTotalsCalculator.calculate(
                List.of(new MemberKindInfo(1, true), new MemberKindInfo(2, false)),
                List.of(new IncomeLine(1, m("5000.00"))),
                List.of(line(ExpenseSource.FIXED, 1, null, "1000.00", 1), line(ExpenseSource.VARIABLE, 1, null, "200.00", 1),
                        line(ExpenseSource.CARD, 1, null, "300.00", 1), line(ExpenseSource.CARD, 2, null, "400.00", 1)),
                CarryOvers.NONE);
        MemberTotal house = totals.getFirst();
        assertThat(house.expense()).isEqualTo(m("1500.00"));
        assertThat(house.balance()).isEqualTo(m("3500.00"));
        assertThat(house.percentSpent()).isEqualByComparingTo("30.0");
        MemberTotal mother = totals.get(1);
        assertThat(mother.main()).isFalse();
        assertThat(mother.card()).isEqualTo(m("400.00"));
        assertThat(mother.percentSpent()).isNull();
    }

    @Test
    @DisplayName("RF-28: a member's carry-overs change what is unpaid and the balance, never the expense")
    void memberCarryOvers() {
        CarryOvers carries = new CarryOvers(Map.of(2L, m("50.00")), Map.of(2L, m("250.00")));
        MemberTotal mother = MemberTotalsCalculator.calculate(List.of(new MemberKindInfo(2, false)),
                List.of(new IncomeLine(2, m("100.00"))), List.of(line(ExpenseSource.CARD, 2, null, "300.00", 1)), carries)
                .getFirst();
        assertThat(mother.expense()).isEqualTo(m("300.00"));
        assertThat(mother.carriedIn()).isEqualTo(m("50.00"));
        assertThat(mother.unpaid()).isEqualTo(m("250.00"));
        assertThat(mother.carriedOut()).isEqualTo(m("250.00"));
        assertThat(mother.balance()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("RF-28, XLS-04: carry-overs move third-party reimbursements between periods, not the expense")
    void householdCarryOvers() {
        List<ExpenseLine> lines = List.of(line(ExpenseSource.CARD, 1, null, "700.00", 1),
                line(ExpenseSource.CARD, 2, null, "300.00", 1));
        List<IncomeLine> incomes = List.of(new IncomeLine(1, m("5000.00")), new IncomeLine(2, m("100.00")));
        CarryOvers carries = new CarryOvers(Map.of(2L, m("40.00")), Map.of(2L, m("240.00")));
        HouseholdTotals household = HouseholdTotals.of(incomes, lines, Set.of(2L), null, carries);
        assertThat(household.reimbursements()).isEqualTo(m("100.00"));
        assertThat(household.income()).isEqualTo(m("5100.00"));
        assertThat(household.expense()).isEqualTo(m("1000.00"));
        assertThat(HouseholdTotals.of(incomes, lines, Set.of(2L), 2L, carries).reimbursements()).isEqualTo(Money.ZERO);
    }

    @Test
    @DisplayName("RF-14, RF-17: projected installments of the next three periods and per card")
    void committed() {
        List<FutureInstallment> future = List.of(new FutureInstallment(NOV.next(), 1, m("100.00")),
                new FutureInstallment(NOV.plusMonths(3), 1, m("50.00")), new FutureInstallment(NOV.plusMonths(4), 2, m("70.00")),
                new FutureInstallment(NOV.next(), 2, m("10.00")), new FutureInstallment(NOV, 1, m("999.00")));
        assertThat(CommittedInstallmentsCalculator.nextPeriods(NOV, future))
                .extracting(p -> p.amount().amount().toPlainString()).containsExactly("110.00", "0.00", "50.00");
        assertThat(CommittedInstallmentsCalculator.byCard(NOV, future)).containsEntry(1L, m("150.00")).containsEntry(2L, m("80.00"));
    }

    @Test
    @DisplayName("RF-27: who owes whom is netted per pair of members")
    void settlements() {
        List<Settlement> result = SettlementCalculator.calculate(List.of(
                line(ExpenseSource.CARD, 2, null, "300.00", 1), line(ExpenseSource.CARD, 1, null, "50.00", 2),
                line(ExpenseSource.CARD, 1, null, "999.00", 1), line(ExpenseSource.CARD, 3, null, "20.00", 1),
                line(ExpenseSource.CARD, 4, null, "10.00", 1), line(ExpenseSource.CARD, 1, null, "10.00", 4)));
        assertThat(result).containsExactly(new Settlement(2, 1, m("250.00")), new Settlement(3, 1, m("20.00")));
    }

    @Test
    @DisplayName("RF-37: card total split into one-off, installments and subscriptions")
    void breakdown() {
        CardBillBreakdown b = CardBillBreakdown.EMPTY.plus("SINGLE", m("10.00")).plus("INSTALLMENT", m("20.00"))
                .plus("SUBSCRIPTION", m("30.00")).plus("SINGLE", m("-5.00"));
        assertThat(b.single()).isEqualTo(m("5.00"));
        assertThat(b.total()).isEqualTo(m("55.00"));
        assertThat(Percentages.variation(m("50.00"), m("-100.00"))).isEqualByComparingTo(new BigDecimal("150.0"));
    }
}

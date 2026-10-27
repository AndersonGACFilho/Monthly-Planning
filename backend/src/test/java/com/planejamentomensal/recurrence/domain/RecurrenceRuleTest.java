package com.planejamentomensal.recurrence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecurrenceRuleTest {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);
    private static final BillingPeriod DEC = BillingPeriod.of(2026, 12);

    private static RuleDetails details(Recurrence recurrence, int interval, BillingPeriod until) {
        return new RuleDetails("Salário", 1L, null, IncomeType.SALARY, Money.of("5000.00"), recurrence, interval, 5, OCT,
                until, PaymentMethod.PIX);
    }

    @Test
    @DisplayName("RF-21: monthly income applies from its start to its optional end")
    void monthly() {
        RecurrenceRule rule = new RecurrenceRule(RuleKind.INCOME, details(Recurrence.MONTHLY, 1, NOV));
        assertThat(rule.appliesTo(BillingPeriod.of(2026, 9))).isFalse();
        assertThat(rule.appliesTo(OCT)).isTrue();
        assertThat(rule.appliesTo(NOV)).isTrue();
        assertThat(rule.appliesTo(DEC)).isFalse();
        assertThat(rule.incomeType()).isEqualTo(IncomeType.SALARY);
        assertThat(rule.paymentMethod()).isNull();
    }

    @Test
    @DisplayName("RF-23: a one-off income applies only to its own period")
    void once() {
        RecurrenceRule rule = new RecurrenceRule(RuleKind.INCOME, details(Recurrence.ONCE, 1, null));
        assertThat(rule.appliesTo(OCT)).isTrue();
        assertThat(rule.appliesTo(NOV)).isFalse();
        assertThat(rule.validUntil()).isEqualTo(OCT);
    }

    @Test
    @DisplayName("RF-21: every N months applies every N periods from the start")
    void everyN() {
        RecurrenceRule rule = new RecurrenceRule(RuleKind.INCOME, details(Recurrence.EVERY_N_MONTHS, 2, null));
        assertThat(rule.appliesTo(OCT)).isTrue();
        assertThat(rule.appliesTo(NOV)).isFalse();
        assertThat(rule.appliesTo(DEC)).isTrue();
        assertThat(rule.intervalMonths()).isEqualTo(2);
    }

    @Test
    @DisplayName("RF-30: fixed expense keeps category and payment method, inactive rules stop generating")
    void fixedExpense() {
        RecurrenceRule rule = new RecurrenceRule(RuleKind.FIXED_EXPENSE, new RuleDetails("Aluguel", 1L, 7L, null,
                Money.of("1800.00"), Recurrence.MONTHLY, 1, 10, OCT, null, PaymentMethod.BOLETO));
        assertThat(rule.categoryId()).isEqualTo(7L);
        assertThat(rule.paymentMethod()).isEqualTo(PaymentMethod.BOLETO);
        assertThat(rule.incomeType()).isNull();
        rule.setActive(false);
        assertThat(rule.active()).isFalse();
        assertThat(rule.appliesTo(OCT)).isFalse();
    }

    @Test
    @DisplayName("RF-21: invalid day, interval or validity is rejected with a code")
    void invalid() {
        assertThatThrownBy(() -> new RecurrenceRule(RuleKind.INCOME, new RuleDetails("x", 1L, null, null, Money.ZERO,
                Recurrence.MONTHLY, 1, 32, OCT, null, null)))
                .isInstanceOf(InvalidInputException.class).hasFieldOrPropertyWithValue("code", "rule-day-invalid");
        assertThatThrownBy(() -> new RecurrenceRule(RuleKind.INCOME, details(Recurrence.EVERY_N_MONTHS, 1, null)))
                .hasFieldOrPropertyWithValue("code", "rule-interval-invalid");
        assertThatThrownBy(() -> new RecurrenceRule(RuleKind.INCOME, new RuleDetails("x", 1L, null, null, Money.ZERO,
                Recurrence.MONTHLY, 1, 1, NOV, OCT, null)))
                .hasFieldOrPropertyWithValue("code", "rule-validity-invalid");
    }

    @Test
    @DisplayName("RF-22: an occurrence follows the default amount until it is changed for its period")
    void occurrenceAmount() {
        RecurrenceRule rule = new RecurrenceRule(RuleKind.INCOME, details(Recurrence.MONTHLY, 1, null));
        Occurrence occurrence = new Occurrence(rule, OCT);
        assertThat(occurrence.amount()).isEqualTo(Money.of("5000.00"));
        occurrence.followDefault(Money.of("5200.00"));
        assertThat(occurrence.amount()).isEqualTo(Money.of("5200.00"));
        occurrence.overrideAmount(Money.of("4800.00"));
        occurrence.followDefault(Money.of("5300.00"));
        assertThat(occurrence.amount()).isEqualTo(Money.of("4800.00"));
        assertThat(occurrence.amountOverridden()).isTrue();
        assertThat(occurrence.billingPeriod()).isEqualTo(OCT);
        assertThat(occurrence.rule()).isSameAs(rule);
    }

    @Test
    @DisplayName("RF-24, RF-32: received or paid with date, back to expected clears the date")
    void status() {
        Occurrence occurrence = new Occurrence(new RecurrenceRule(RuleKind.INCOME, details(Recurrence.MONTHLY, 1, null)), OCT);
        occurrence.markDone(LocalDate.of(2026, 10, 5));
        assertThat(occurrence.status()).isEqualTo(OccurrenceStatus.DONE);
        assertThat(occurrence.effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 5));
        occurrence.followDefault(Money.of("1.00"));
        assertThat(occurrence.amount()).isEqualTo(Money.of("5000.00"));
        occurrence.markExpected();
        assertThat(occurrence.status()).isEqualTo(OccurrenceStatus.EXPECTED);
        assertThat(occurrence.effectiveDate()).isNull();
    }

    @Test
    @DisplayName("RF-33, RN-30: linking to an entry marks the expense paid on the entry date and clears candidates")
    void link() {
        Occurrence occurrence = new Occurrence(new RecurrenceRule(RuleKind.FIXED_EXPENSE, details(Recurrence.MONTHLY, 1, null)), OCT);
        occurrence.offerCandidates("3,4");
        assertThat(occurrence.linkCandidates()).isEqualTo("3,4");
        occurrence.linkTo(4L, LocalDate.of(2026, 9, 20));
        assertThat(occurrence.linkedEntryId()).isEqualTo(4L);
        assertThat(occurrence.linkCandidates()).isNull();
        assertThat(occurrence.status()).isEqualTo(OccurrenceStatus.DONE);
        assertThat(occurrence.effectiveDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }
}

package com.planejamentomensal.recurrence.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecurrenceForecastTest {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);
    private static final BillingPeriod DEC = BillingPeriod.of(2026, 12);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private RecurrenceCommands commands;
    @Autowired private RecurrenceForecast forecast;
    @Autowired private OccurrenceRepository occurrences;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "8");
    }

    private RecurrenceRule rent(Recurrence recurrence) {
        return commands.create(RuleKind.FIXED_EXPENSE, new RuleDetails("Aluguel", f.main, null, null, Money.of("1800.00"),
                recurrence, 1, 10, NOV, null, PaymentMethod.BOLETO));
    }

    @Test
    @DisplayName("IMP-64: a fixed expense is projected into a future period without writing its month")
    void projectsWithoutWriting() {
        RecurrenceRule rule = rent(Recurrence.MONTHLY);
        assertThat(forecast.fixedExpenses(DEC)).singleElement().satisfies(v -> {
            assertThat(v.description()).isEqualTo("Aluguel");
            assertThat(v.amount()).isEqualTo(Money.of("1800.00"));
            assertThat(v.billingPeriod()).isEqualTo(DEC);
            assertThat(v.done()).isFalse();
        });
        assertThat(occurrences.findByRuleId(rule.id())).isEmpty();
    }

    @Test
    @DisplayName("IMP-64: the amount changed for a month wins over the default")
    void storedMonthWins() {
        rent(Recurrence.MONTHLY);
        Occurrence december = commands.occurrences(DEC, RuleKind.FIXED_EXPENSE).getFirst();
        commands.overrideAmount(december.id(), Money.of("1500.00"), december.version());
        assertThat(forecast.fixedExpenses(DEC)).singleElement()
                .satisfies(v -> assertThat(v.amount()).isEqualTo(Money.of("1500.00")));
    }

    @Test
    @DisplayName("IMP-64: incomes, inactive and one-off rules are not projected past their months")
    void skipsRulesThatDoNotApply() {
        rent(Recurrence.ONCE);
        commands.create(RuleKind.INCOME, new RuleDetails("Salário", f.main, null, IncomeType.SALARY, Money.of("5000.00"),
                Recurrence.MONTHLY, 1, 5, NOV, null, null));
        commands.setActive(rent(Recurrence.MONTHLY).id(), false);
        assertThat(forecast.fixedExpenses(DEC)).isEmpty();
    }
}

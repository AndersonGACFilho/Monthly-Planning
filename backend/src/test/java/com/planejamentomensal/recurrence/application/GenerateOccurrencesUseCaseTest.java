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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GenerateOccurrencesUseCaseTest {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private GenerateOccurrencesUseCase generator;
    @Autowired private RecurrenceCommands commands;
    @Autowired private OccurrenceRepository occurrences;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "7");
    }

    private RuleDetails income(String description, String amount, Recurrence recurrence) {
        return new RuleDetails(description, f.main, null, IncomeType.SALARY, Money.of(amount), recurrence, 1, 5, OCT, null,
                null);
    }

    @Test
    @DisplayName("IMP-29, RF-22: incomes are generated once per period, by description")
    void generatesOnce() {
        commands.create(RuleKind.INCOME, income("Salário", "5000.00", Recurrence.MONTHLY));
        commands.create(RuleKind.INCOME, income("Bolsa", "1200.00", Recurrence.ONCE));
        List<Occurrence> oct = generator.generate(OCT, RuleKind.INCOME);
        assertThat(oct).extracting(o -> o.rule().description()).containsExactly("Bolsa", "Salário");
        assertThat(generator.generate(OCT, RuleKind.INCOME)).extracting(Occurrence::id)
                .containsExactlyElementsOf(oct.stream().map(Occurrence::id).toList());
        assertThat(generator.generate(NOV, RuleKind.INCOME)).extracting(o -> o.rule().description()).containsExactly("Salário");
        assertThat(generator.generate(OCT, RuleKind.FIXED_EXPENSE)).isEmpty();
    }

    @Test
    @DisplayName("RF-22, RF-31: a new default amount reaches expected periods but not those changed by hand")
    void followsDefault() {
        RecurrenceRule rule = commands.create(RuleKind.FIXED_EXPENSE, new RuleDetails("Aluguel", f.main, null, null,
                Money.of("1800.00"), Recurrence.MONTHLY, 1, 10, OCT, null, PaymentMethod.BOLETO));
        Occurrence oct = generator.generate(OCT, RuleKind.FIXED_EXPENSE).getFirst();
        Occurrence nov = generator.generate(NOV, RuleKind.FIXED_EXPENSE).getFirst();
        commands.overrideAmount(oct.id(), Money.of("1750.00"), oct.version());
        commands.update(rule.id(), new RuleDetails("Aluguel", f.main, null, null, Money.of("1900.00"), Recurrence.MONTHLY, 1,
                10, OCT, null, PaymentMethod.BOLETO), rule.version());
        assertThat(generator.generate(OCT, RuleKind.FIXED_EXPENSE).getFirst().amount()).isEqualTo(Money.of("1750.00"));
        assertThat(generator.generate(NOV, RuleKind.FIXED_EXPENSE).getFirst().amount()).isEqualTo(Money.of("1900.00"));
        assertThat(occurrences.findByRuleId(rule.id())).hasSize(2).extracting(Occurrence::id).contains(nov.id());
    }

    @Test
    @DisplayName("RF-21: an inactive rule keeps its generated periods and stops generating new ones")
    void inactive() {
        RecurrenceRule rule = commands.create(RuleKind.INCOME, income("Freela", "800.00", Recurrence.MONTHLY));
        generator.generate(OCT, RuleKind.INCOME);
        commands.setActive(rule.id(), false);
        assertThat(generator.generate(OCT, RuleKind.INCOME)).hasSize(1);
        assertThat(generator.generate(NOV, RuleKind.INCOME)).isEmpty();
        assertThat(commands.rules(RuleKind.INCOME)).extracting(RecurrenceRule::active).containsExactly(false);
    }
}

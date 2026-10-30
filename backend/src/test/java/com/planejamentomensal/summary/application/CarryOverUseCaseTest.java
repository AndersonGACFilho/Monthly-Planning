package com.planejamentomensal.summary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator.MemberTotal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CarryOverUseCaseTest {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);
    private static final BillingPeriod DEC = NOV.next();

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entryCommands;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;
    @Autowired private MemberQueries memberQueries;
    @Autowired private MonthlySummaryService summaries;
    @Autowired private CarryOverUseCase useCase;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "8");
        recurrence.create(RuleKind.INCOME, new RuleDetails("Salário", f.main, null, IncomeType.SALARY, Money.of("5000.00"),
                Recurrence.MONTHLY, 1, 5, NOV, null, null));
        BillEntry hers = ledger.addPurchase(null, f.purchase("FARMACIA", "300.00", LocalDate.of(2026, 10, 5), NOV, null, null,
                "PURCHASE"));
        entryCommands.classify(hers.id(), null, false, List.of(new ReviewItem.Share(f.thirdParty, new BigDecimal("100"), null)),
                null, null);
        recurrence.create(RuleKind.INCOME, new RuleDetails("Pix da tia", f.thirdParty, null, IncomeType.OTHER,
                Money.of("100.00"), Recurrence.ONCE, 1, 15, NOV, null, null));
    }

    private MemberTotal row(BillingPeriod period) {
        return summaries.summary(period, null).members().stream().filter(m -> m.memberId() == f.thirdParty).findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("RF-28: the unpaid amount moves the reimbursement to the next period and keeps each period's expense")
    void carriesUnpaidAmount() {
        MonthlySummary before = summaries.summary(NOV, null);
        assertThat(useCase.carry(NOV, List.of(f.thirdParty))).singleElement()
                .satisfies(c -> assertThat(c.amount()).isEqualTo(Money.of("200.00")));

        MonthlySummary nov = summaries.summary(NOV, null);
        assertThat(nov.indicators().expense()).isEqualTo(before.indicators().expense());
        assertThat(nov.reimbursements()).isEqualTo(Money.of("100.00"));
        assertThat(row(NOV).carriedOut()).isEqualTo(Money.of("200.00"));
        assertThat(row(NOV).balance()).isEqualTo(Money.ZERO);

        MonthlySummary dec = summaries.summary(DEC, null);
        assertThat(dec.indicators().expense()).isEqualTo(Money.ZERO);
        assertThat(dec.reimbursements()).isEqualTo(Money.of("200.00"));
        assertThat(row(DEC).carriedIn()).isEqualTo(Money.of("200.00"));
        assertThat(row(DEC).unpaid()).isEqualTo(Money.of("200.00"));
    }

    @Test
    @DisplayName("RF-28: carrying again after the period changed replaces the amount")
    void carryingAgainUpdates() {
        useCase.carry(NOV, List.of(f.thirdParty));
        expenses.create(new ExpenseDetails(LocalDate.of(2026, 11, 3), null, "Feira", f.thirdParty, null, Money.of("50.00"),
                PaymentMethod.PIX));
        assertThat(row(NOV).unpaid()).isEqualTo(Money.of("250.00"));
        assertThat(useCase.carry(NOV, List.of(f.thirdParty))).singleElement()
                .satisfies(c -> assertThat(c.amount()).isEqualTo(Money.of("250.00")));
        assertThat(row(DEC).carriedIn()).isEqualTo(Money.of("250.00"));
    }

    @Test
    @DisplayName("RF-28: only third parties who still owe something can carry it over")
    void rejectsMainAndSettledMembers() {
        assertThatThrownBy(() -> useCase.carry(NOV, List.of(f.main))).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "carry-over-main-member");
        recurrence.create(RuleKind.INCOME, new RuleDetails("Resto", f.thirdParty, null, IncomeType.OTHER, Money.of("200.00"),
                Recurrence.ONCE, 1, 20, NOV, null, null));
        assertThatThrownBy(() -> useCase.carry(NOV, List.of(f.thirdParty))).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "carry-over-nothing-due");
    }

    @Test
    @DisplayName("RF-28, RF-01: a carry-over carried on again is locked, and its member cannot be deleted")
    void chainsAreLocked() {
        useCase.carry(NOV, List.of(f.thirdParty));
        useCase.carry(DEC, List.of(f.thirdParty));
        assertThat(row(DEC).carriedOut()).isEqualTo(Money.of("200.00"));
        assertThat(memberQueries.isDeletable(f.thirdParty)).isFalse();
        assertThatThrownBy(() -> useCase.undo(NOV, f.thirdParty)).isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "carry-over-chained");
        useCase.undo(DEC, f.thirdParty);
        useCase.undo(NOV, f.thirdParty);
        assertThat(row(DEC).carriedIn()).isEqualTo(Money.ZERO);
    }
}

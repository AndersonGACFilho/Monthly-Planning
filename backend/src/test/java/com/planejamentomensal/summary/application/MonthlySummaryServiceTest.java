package com.planejamentomensal.summary.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.summary.domain.Indicators;
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
class MonthlySummaryServiceTest {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entryCommands;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;
    @Autowired private MonthlySummaryService service;
    @Autowired private CardBillSummaries cardSummaries;

    private LedgerFixture f;

    private BillEntry purchase(String description, String amount, BillingPeriod period, Integer n, Integer count) {
        return ledger.addPurchase(null, f.purchase(description, amount, LocalDate.of(2026, 10, 5), period, n, count, "PURCHASE"));
    }

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "6");
        recurrence.create(RuleKind.INCOME, new RuleDetails("Salário", f.main, null, IncomeType.SALARY, Money.of("10000.00"),
                Recurrence.MONTHLY, 1, 5, OCT, null, null));
        recurrence.create(RuleKind.FIXED_EXPENSE, new RuleDetails("Aluguel", f.main, null, null, Money.of("2000.00"),
                Recurrence.MONTHLY, 1, 10, OCT, null, PaymentMethod.BOLETO));
        recurrence.create(RuleKind.FIXED_EXPENSE, new RuleDetails("NETFLIX.COM", f.main, null, null, Money.of("55.90"),
                Recurrence.MONTHLY, 1, 10, OCT, null, PaymentMethod.CARD));
        expenses.create(new ExpenseDetails(LocalDate.of(2026, 11, 3), null, "Feira", f.main, null, Money.of("300.00"),
                PaymentMethod.PIX));
        purchase("NETFLIX.COM", "55.90", NOV, null, null);
        BillEntry shared = purchase("MERCADO", "100.00", NOV, null, null);
        entryCommands.classify(shared.id(), null, false, List.of(new ReviewItem.Share(f.main, new BigDecimal("50"), null),
                new ReviewItem.Share(f.thirdParty, new BigDecimal("50"), null)), null, null);
        BillEntry hers = purchase("FARMACIA", "200.00", NOV, null, null);
        entryCommands.classify(hers.id(), null, false, List.of(new ReviewItem.Share(f.thirdParty, new BigDecimal("100"), null)),
                null, null);
        purchase("TV LOJA", "90.00", NOV, 1, 3);
    }

    @Test
    @DisplayName("RF-10, RF-11, XLS-04: household income adds third-party reimbursements; card-paid fixed expenses count once")
    void household() {
        MonthlySummary s = service.summary(NOV, null);
        assertThat(s.fixedTotal()).isEqualTo(Money.of("2000.00"));
        assertThat(s.variableTotal()).isEqualTo(Money.of("300.00"));
        assertThat(s.cardTotal()).isEqualTo(Money.of("445.90"));
        assertThat(s.mainIncome()).isEqualTo(Money.of("10000.00"));
        assertThat(s.reimbursements()).isEqualTo(Money.of("250.00"));
        assertThat(s.indicators().income()).isEqualTo(Money.of("10250.00"));
        assertThat(s.indicators().expense()).isEqualTo(Money.of("2745.90"));
        assertThat(s.indicators().balance()).isEqualTo(Money.of("7504.10"));
        assertThat(s.indicators().alert()).isEqualTo(Indicators.AlertLevel.NONE);
        assertThat(s.settlements()).singleElement().satisfies(t -> {
            assertThat(t.fromMemberId()).isEqualTo(f.thirdParty);
            assertThat(t.toMemberId()).isEqualTo(f.main);
            assertThat(t.amount()).isEqualTo(Money.of("250.00"));
        });
        assertThat(s.committedNext()).extracting(p -> p.amount().amount().toPlainString())
                .containsExactly("90.00", "90.00", "0.00");
        assertThat(s.pendingCount()).isEqualTo(4);
        assertThat(s.pendingAmount()).isEqualTo(Money.of("445.90"));
    }

    @Test
    @DisplayName("RF-15, RF-25: the member filter keeps only that member's incomes and shares")
    void memberFilter() {
        MonthlySummary mother = service.summary(NOV, f.thirdParty);
        assertThat(mother.cardTotal()).isEqualTo(Money.of("250.00"));
        assertThat(mother.indicators().income()).isEqualTo(Money.ZERO);
        assertThat(mother.reimbursements()).isEqualTo(Money.ZERO);
        assertThat(mother.members()).singleElement().satisfies(m -> assertThat(m.card()).isEqualTo(Money.of("250.00")));
        assertThat(mother.cards()).singleElement().satisfies(c -> assertThat(c.total()).isEqualTo(Money.of("300.00")));
        MonthlySummary house = service.summary(NOV, f.main);
        assertThat(house.indicators().income()).isEqualTo(Money.of("10000.00"));
        assertThat(house.cardTotal()).isEqualTo(Money.of("195.90"));
    }

    @Test
    @DisplayName("RF-36, RF-37, RF-14: card total with forecast dates, breakdown and future installments")
    void cardTotals() {
        CardBillSummary card = cardSummaries.forPeriod(NOV).stream().filter(c -> c.cardId() == f.card).findFirst().orElseThrow();
        assertThat(card.total()).isEqualTo(Money.of("445.90"));
        assertThat(card.breakdown().installments()).isEqualTo(Money.of("90.00"));
        assertThat(card.breakdown().single()).isEqualTo(Money.of("355.90"));
        assertThat(card.committedFuture()).isEqualTo(Money.of("180.00"));
        assertThat(card.bill().dueDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(card.bill().closingDate()).isEqualTo(LocalDate.of(2026, 11, 3));
        assertThat(card.bill().status()).isEqualTo("OPEN");
        CardBillSummary empty = cardSummaries.forPeriod(BillingPeriod.of(2027, 6)).stream()
                .filter(c -> c.cardId() == f.card).findFirst().orElseThrow();
        assertThat(empty.total()).isEqualTo(Money.ZERO);
        assertThat(empty.bill().cardBillId()).isNull();
    }
}

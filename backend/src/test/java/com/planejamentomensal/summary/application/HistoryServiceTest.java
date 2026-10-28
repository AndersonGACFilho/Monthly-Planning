package com.planejamentomensal.summary.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.category.application.SaveCategoryUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HistoryServiceTest {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private SaveCategoryUseCase categories;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entryCommands;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private HistoryService service;

    private LedgerFixture f;
    private long house;
    private long market;
    private long leisure;

    private void purchase(String description, String amount, BillingPeriod period, long category, long member) {
        BillEntry entry = ledger.addPurchase(null, f.purchase(description, amount, LocalDate.of(2026, 9, 5), period, null, null,
                "PURCHASE"));
        entryCommands.classify(entry.id(), category, false, List.of(new ReviewItem.Share(member, new BigDecimal("100"), null)),
                null, null);
    }

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "7");
        house = categories.create("Casa histórico", null, "#888888", "tag").id();
        market = categories.create("Mercado histórico", house, "#888888", "tag").id();
        leisure = categories.create("Lazer histórico", null, "#888888", "tag").id();
        recurrence.create(RuleKind.INCOME, new RuleDetails("Salário", f.main, null, IncomeType.SALARY, Money.of("10000.00"),
                Recurrence.MONTHLY, 1, 5, OCT, null, null));
        purchase("MERCADO", "100.00", OCT, market, f.main);
        purchase("CINEMA", "50.00", NOV, leisure, f.main);
        purchase("FARMACIA", "200.00", NOV, market, f.thirdParty);
    }

    @Test
    @DisplayName("RF-10, RF-11: one point per month with the overview totals")
    void household() {
        List<HistoryPoint> points = service.history(OCT, NOV, null, null);
        assertThat(points).extracting(HistoryPoint::period).containsExactly(OCT, NOV);
        assertThat(points.get(0).expense()).isEqualTo(Money.of("100.00"));
        assertThat(points.get(0).balance()).isEqualTo(Money.of("9900.00"));
        HistoryPoint nov = points.get(1);
        assertThat(nov.income()).isEqualTo(Money.of("10200.00"));
        assertThat(nov.reimbursements()).isEqualTo(Money.of("200.00"));
        assertThat(nov.card()).isEqualTo(Money.of("250.00"));
        assertThat(nov.byMember()).isEqualTo(Map.of(f.main, Money.of("50.00"), f.thirdParty, Money.of("200.00")));
    }

    @Test
    @DisplayName("RF-15, RF-25: a parent category filter takes its subcategories; by-category ignores it")
    void categoryFilter() {
        List<HistoryPoint> points = service.history(OCT, NOV, null, house);
        assertThat(points).extracting(HistoryPoint::expense).containsExactly(Money.of("100.00"), Money.of("200.00"));
        assertThat(points.get(1).byMember()).isEqualTo(Map.of(f.thirdParty, Money.of("200.00")));
        assertThat(points.get(1).byCategory()).containsEntry(leisure, Money.of("50.00")).containsEntry(market, Money.of("200.00"));
        assertThat(service.history(NOV, NOV, null, leisure)).singleElement()
                .satisfies(p -> assertThat(p.expense()).isEqualTo(Money.of("50.00")));
    }

    @Test
    @DisplayName("RF-15: the member filter keeps that member's incomes and shares")
    void memberFilter() {
        HistoryPoint nov = service.history(NOV, NOV, f.thirdParty, null).getFirst();
        assertThat(nov.expense()).isEqualTo(Money.of("200.00"));
        assertThat(nov.income()).isEqualTo(Money.ZERO);
        assertThat(nov.byCategory()).isEqualTo(Map.of(market, Money.of("200.00")));
    }

    @Test
    @DisplayName("RF-15: a backwards or too long range is rejected")
    void range() {
        assertThatThrownBy(() -> service.history(NOV, OCT, null, null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.history(OCT, OCT.plusMonths(HistoryService.MAX_MONTHS), null, null))
                .isInstanceOf(InvalidInputException.class);
    }
}

package com.planejamentomensal.recurrence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FixedExpenseLinkerTest {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private RecurrenceCommands commands;

    private LedgerFixture f;

    @BeforeEach
    void setUp() {
        f = new LedgerFixture(members, cards, "8");
    }

    private BillEntry entry(long batch, String description, String amount, int day) {
        return ledger.addPurchase(batch, f.purchase(description, amount, LocalDate.of(2026, 10, day), NOV, null, null, "PURCHASE"));
    }

    private void expense(String description, String amount, PaymentMethod method) {
        commands.create(RuleKind.FIXED_EXPENSE, new RuleDetails(description, f.main, null, null, Money.of(amount),
                Recurrence.MONTHLY, 1, 10, NOV, null, method));
    }

    private Occurrence only(String description) {
        return commands.occurrences(NOV, RuleKind.FIXED_EXPENSE).stream()
                .filter(o -> o.rule().description().equals(description)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("RF-33: a card-paid fixed expense links to the bill entry with the same description and close amount")
    void linksSingleCandidate() {
        BillEntry netflix = entry(1L, "NETFLIX.COM", "55.90", 12);
        entry(1L, "SPOTIFY", "55.90", 13);
        expense("Netflix.com", "52.00", PaymentMethod.CARD);
        expense("Aluguel", "1800.00", PaymentMethod.BOLETO);
        Occurrence linked = only("Netflix.com");
        assertThat(linked.linkedEntryId()).isEqualTo(netflix.id());
        assertThat(linked.status()).isEqualTo(OccurrenceStatus.DONE);
        assertThat(linked.effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(only("Aluguel").linkedEntryId()).isNull();
    }

    @Test
    @DisplayName("RF-33: an amount more than 10% away is not linked")
    void farAmount() {
        entry(1L, "NETFLIX.COM", "70.00", 12);
        expense("NETFLIX.COM", "55.90", PaymentMethod.CARD);
        Occurrence occurrence = only("NETFLIX.COM");
        assertThat(occurrence.linkedEntryId()).isNull();
        assertThat(occurrence.linkCandidates()).isNull();
        assertThat(occurrence.status()).isEqualTo(OccurrenceStatus.EXPECTED);
    }

    @Test
    @DisplayName("RN-30: several candidates wait for the user's choice; only a candidate can be chosen")
    void severalCandidates() {
        BillEntry first = entry(1L, "ACADEMIA FIT", "99.90", 3);
        BillEntry second = entry(2L, "ACADEMIA FIT", "99.90", 20);
        expense("Academia Fit", "99.90", PaymentMethod.CARD);
        Occurrence occurrence = only("Academia Fit");
        assertThat(occurrence.linkedEntryId()).isNull();
        assertThat(occurrence.linkCandidates()).isEqualTo(first.id() + "," + second.id());
        assertThat(commands.pendingLinks()).extracting(Occurrence::id).containsExactly(occurrence.id());
        assertThatThrownBy(() -> commands.chooseLink(occurrence.id(), 999_999L))
                .isInstanceOf(BusinessRuleException.class).hasFieldOrPropertyWithValue("code", "expense-link-invalid");
        Occurrence chosen = commands.chooseLink(occurrence.id(), second.id());
        assertThat(chosen.linkedEntryId()).isEqualTo(second.id());
        assertThat(chosen.effectiveDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(commands.pendingLinks()).isEmpty();
    }
}

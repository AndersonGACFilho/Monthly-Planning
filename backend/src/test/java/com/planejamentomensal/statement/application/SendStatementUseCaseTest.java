package com.planejamentomensal.statement.application;

import static com.planejamentomensal.statement.application.StatementFixture.NOV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.nio.file.Files;
import java.nio.file.Path;
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
class SendStatementUseCaseTest {

    @Autowired private SaveMemberUseCase members;
    @Autowired private MemberQueries memberQueries;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entries;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;
    @Autowired private SendStatementUseCase send;
    @Autowired private StatementDeliveries history;

    private StatementFixture s;

    @BeforeEach
    void setUp() {
        s = new StatementFixture(members, cards, ledger, entries, recurrence, expenses, "3");
    }

    private SendRequest request(List<Long> ids, DeliveryChannelType channel, String language, boolean anyway) {
        return new SendRequest(NOV, ids, channel, StatementOptions.DEFAULT, language, null, null, anyway);
    }

    @Test
    @DisplayName("RF-79b: pending entries stop the delivery unless the user sends anyway")
    void pendingWarning() {
        assertThatThrownBy(() -> send.send(request(List.of(s.f.thirdParty), DeliveryChannelType.FOLDER, null, false)))
                .isInstanceOf(BusinessRuleException.class).hasFieldOrPropertyWithValue("code", "statement-has-pending");
        assertThat(send.send(request(List.of(s.f.thirdParty), DeliveryChannelType.FOLDER, null, true)))
                .singleElement().satisfies(o -> assertThat(o.status()).isEqualTo(DeliveryStatus.SENT));
    }

    @Test
    @DisplayName("RF-89, RF-91, RF-92, I18N-12: the folder channel saves the PDF; history keeps template version and language")
    void folderAndHistory() throws Exception {
        List<DeliveryOutcome> outcomes = send.send(request(List.of(s.f.thirdParty, s.f.main), DeliveryChannelType.FOLDER, "en-US", true));
        assertThat(outcomes).extracting(DeliveryOutcome::status).containsOnly(DeliveryStatus.SENT);
        Path pdf = Path.of(outcomes.getFirst().detail());
        assertThat(pdf.getFileName().toString()).isEqualTo("statement-t3-2026-11.pdf");
        assertThat(pdf.toString()).contains("exports").contains("2026-11");
        assertThat(Files.readAllBytes(pdf)).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        assertThat(history.history(NOV, s.f.thirdParty)).singleElement().satisfies(r -> {
            assertThat(r.delivery().language()).isEqualTo("en-US");
            assertThat(r.delivery().templateVersion()).isEqualTo(1);
            assertThat(r.outdated()).isFalse();
        });
    }

    @Test
    @DisplayName("RF-93: a change in the period after sending marks the delivery as outdated")
    void outdated() {
        send.send(request(List.of(s.f.thirdParty), DeliveryChannelType.FOLDER, null, true));
        expenses.create(new ExpenseDetails(LocalDate.of(2026, 11, 20), null, "Táxi", s.f.thirdParty, null, Money.of("30.00"),
                PaymentMethod.PIX));
        assertThat(history.history(null, s.f.thirdParty)).singleElement().satisfies(r -> assertThat(r.outdated()).isTrue());
    }

    @Test
    @DisplayName("RF-87, SOL-03, I18N-08: WhatsApp waits for the user with a wa.me link in the member language; then marked sent")
    void whatsapp() {
        long tia = members.create(new MemberDetails("W3", "Wanda", MemberKind.THIRD_PARTY, "#000000", null, "+55 (51) 99999-0000",
                null, "en-US")).id();
        DeliveryOutcome o = send.send(request(List.of(tia), DeliveryChannelType.WHATSAPP, null, true)).getFirst();
        assertThat(o.status()).isEqualTo(DeliveryStatus.AWAITING_USER);
        assertThat(o.action()).startsWith("https://wa.me/5551999990000?text=Hi%20Wanda");
        assertThat(o.detail()).endsWith(".png");
        assertThat(history.markSent(o.deliveryId()).status()).isEqualTo(DeliveryStatus.SENT);
        assertThatThrownBy(() -> history.markSent(o.deliveryId())).hasFieldOrPropertyWithValue("code", "delivery-not-awaiting");
    }

    @Test
    @DisplayName("RF-91: a member who cannot receive fails alone; the others go on; resend repeats the delivery")
    void batchFailure() {
        long noPhone = members.create(new MemberDetails("N3", "Nina", MemberKind.THIRD_PARTY, "#000000", null, null, null,
                "pt-BR")).id();
        assertThat(memberQueries.find(noPhone)).isPresent();
        List<DeliveryOutcome> outcomes = send.send(request(List.of(noPhone), DeliveryChannelType.WHATSAPP, null, true));
        assertThat(outcomes.getFirst().status()).isEqualTo(DeliveryStatus.FAILED);
        DeliveryOutcome again = send.resend(outcomes.getFirst().deliveryId());
        assertThat(again.status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(again.deliveryId()).isNotEqualTo(outcomes.getFirst().deliveryId());
        List<DeliveryOutcome> email = send.send(request(List.of(s.f.main), DeliveryChannelType.EMAIL, null, true));
        assertThat(email.getFirst().status()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(send.send(request(List.of(s.f.main), DeliveryChannelType.CLIPBOARD, null, true)).getFirst().status())
                .isEqualTo(DeliveryStatus.AWAITING_USER);
    }
}

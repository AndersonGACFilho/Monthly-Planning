package com.planejamentomensal.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.CardDetails;
import com.planejamentomensal.entry.application.BillEntryRepository;
import com.planejamentomensal.entry.application.CardBillRepository;
import com.planejamentomensal.entry.application.PurchaseRepository;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.entry.domain.BillStatus;
import com.planejamentomensal.entry.domain.EntryKind;
import com.planejamentomensal.entry.domain.EntryStatus;
import com.planejamentomensal.entry.domain.ReviewStatus;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.domain.LineStatus;
import com.planejamentomensal.imports.infra.itau.Fixtures;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ImportFlowIT {

    private static final BillingPeriod OCT = BillingPeriod.of(2026, 10);
    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private PreviewImportUseCase preview;
    @Autowired private ConfirmImportUseCase confirm;
    @Autowired private ManageImportUseCase manage;
    @Autowired private ImportQueries queries;
    @Autowired private BillEntryRepository entries;
    @Autowired private PurchaseRepository purchases;
    @Autowired private CardBillRepository bills;

    private long cardId;
    private long memberId;

    @BeforeEach
    void card() {
        memberId = members.create(new MemberDetails("AB", "Casa", MemberKind.MAIN, "#2563EB", null, null, null, "pt-BR")).id();
        cardId = cards.create(new CardDetails("Itaú Platinum", Bank.ITAU, List.of("4102", "4090"), "MARIANA S F SILVA",
                null, memberId), 2, 9).id();
    }

    private ImportBatch importAndConfirm(String fixture, BillingPeriod period) {
        ImportBatch batch = preview.preview(cardId, period, fixture, Fixtures.bytes(fixture));
        return confirm.confirm(batch.id(), false);
    }

    private List<BillEntry> entriesIn(BillingPeriod period) {
        return entries.findByCardBillCardId(cardId).stream().filter(e -> e.cardBill().billingPeriod().equals(period)).toList();
    }

    @Test
    @DisplayName("RF-68, RF-69: preview classifies every line, writes nothing and offers default decisions")
    void previewWritesNothing() {
        ImportBatch batch = preview.preview(cardId, OCT, Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        assertThat(batch.status()).isEqualTo(BatchStatus.PREVIEW);
        assertThat(batch.newCount()).isEqualTo(79);
        assertThat(batch.warnings()).isEmpty();
        assertThat(batch.totalsMatch()).isTrue();
        assertThat(entries.findByCardBillCardId(cardId)).isEmpty();
        List<ImportLine> lines = queries.lines(batch.id());
        assertThat(lines).hasSize(79 + 16);
        assertThat(lines.stream().filter(ImportLine::futureCheck)).hasSize(16);
        assertThat(lines.stream().filter(l -> !l.futureCheck())).allMatch(l -> l.status() == LineStatus.NEW
                && l.memberId() == memberId);
    }

    @Test
    @DisplayName("RF-62: different due month and unknown card final are warned")
    void warnings() {
        ImportBatch batch = preview.preview(cardId, NOV, Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        assertThat(batch.warnings()).contains(PreviewImportUseCase.PERIOD_MISMATCH);
        long other = cards.create(new CardDetails("Outro", Bank.ITAU, List.of("1111"), "X", null, memberId), 2, 9).id();
        assertThat(preview.preview(other, OCT, Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF)).warnings())
                .contains(PreviewImportUseCase.CARD_MISMATCH);
    }

    @Test
    @DisplayName("RF-72, RN-04, RN-05, RF-74: confirming creates pending purchases and projects remaining installments")
    void confirmProjects() {
        ImportBatch batch = importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        assertThat(batch.status()).isEqualTo(BatchStatus.CONFIRMED);
        assertThat(entriesIn(OCT)).hasSize(79).allMatch(e -> e.status() == EntryStatus.CONFIRMED);
        BillEntry amazon = entriesIn(OCT).stream().filter(e -> e.installmentLabel().equals("12/12")).findFirst().orElseThrow();
        assertThat(entries.findByPurchaseIdOrderByEntryDate(amazon.purchase().id())).hasSize(1);
        assertThat(amazon.purchase().firstKnownInstallment()).isEqualTo(12);
        BillEntry bmb = entriesIn(OCT).stream().filter(e -> e.purchase().originalDescription().equals("BMB *Equato")).findFirst().orElseThrow();
        List<BillEntry> bmbInstallments = entries.findByPurchaseIdOrderByEntryDate(bmb.purchase().id());
        assertThat(bmbInstallments).extracting(e -> e.cardBill().billingPeriod()).containsExactly(OCT, NOV);
        assertThat(bmbInstallments.get(1).status()).isEqualTo(EntryStatus.PROJECTED);
        assertThat(purchases.findByReviewStatus(ReviewStatus.PENDING_REVIEW)).hasSize(79);
        var bill = bills.findByCardIdAndBillingPeriod(cardId, OCT).orElseThrow();
        assertThat(bill.status()).isEqualTo(BillStatus.CLOSED);
        assertThat(bill.fileTotal()).isEqualTo(Money.of("6514.13"));
        assertThat(bills.findByCardIdAndBillingPeriod(cardId, NOV).orElseThrow().closingDate()).hasToString("2026-11-02");
    }

    @Test
    @DisplayName("RN-20: projected installments take the amount announced by the bank for the next bills")
    void bankFutureAmountsWin() {
        importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        BillEntry mercado = entriesIn(NOV).stream()
                .filter(e -> e.purchase().originalDescription().equals("MERCADOLIVRE*M")).findFirst().orElseThrow();
        assertThat(mercado.amount()).isEqualTo(Money.of("48.01"));
        assertThat(mercado.status()).isEqualTo(EntryStatus.PROJECTED);
    }

    @Test
    @DisplayName("RN-06, RN-19: the next bill's installment confirms the projected one across formats")
    void nextBillConfirmsProjection() {
        importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        ImportBatch november = preview.preview(cardId, NOV, Fixtures.OPEN_XLSX, Fixtures.bytes(Fixtures.OPEN_XLSX));
        List<ImportLine> lines = queries.lines(november.id());
        assertThat(lines.stream().filter(l -> l.description().startsWith("Bmb")).findFirst().orElseThrow().status())
                .isEqualTo(LineStatus.MATCHED);
        assertThat(lines.stream().filter(l -> l.description().startsWith("Mercadolivre*mercadolj")).findFirst().orElseThrow().status())
                .isEqualTo(LineStatus.MATCHED);
        assertThat(november.matchedCount()).isGreaterThanOrEqualTo(10);
        assertThat(lines.stream().filter(l -> "9103".equals(l.cardLastDigits())).findFirst().orElseThrow().unknownFinal()).isTrue();
        confirm.confirm(november.id(), false);
        BillEntry bmbNov = entriesIn(NOV).stream().filter(e -> e.purchase().originalDescription().equals("BMB *Equato")).findFirst().orElseThrow();
        assertThat(bmbNov.status()).isEqualTo(EntryStatus.CONFIRMED);
        assertThat(bills.findByCardIdAndBillingPeriod(cardId, NOV).orElseThrow().partial()).isTrue();
    }

    @Test
    @DisplayName("RF-63e, RN-28: IOF line is linked to its purchase and unmatched credits stay standalone")
    void iofAndCredits() {
        importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        importAndConfirm(Fixtures.OPEN_XLSX, NOV);
        BillEntry iof = entriesIn(NOV).stream().filter(e -> e.purchase().kind() == EntryKind.IOF).findFirst().orElseThrow();
        BillEntry openai = entries.findById(iof.relatedEntryId()).orElseThrow();
        assertThat(openai.purchase().originalDescription()).startsWith("Openai");
        assertThat(entriesIn(NOV).stream().filter(e -> e.purchase().kind() == EntryKind.CREDIT)).hasSize(2);
    }

    @Test
    @DisplayName("RF-73: importing the same file again creates nothing")
    void idempotent() {
        importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        ImportBatch again = preview.preview(cardId, OCT, Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        assertThat(again.warnings()).contains(PreviewImportUseCase.ALREADY_IMPORTED);
        assertThat(again.duplicateCount()).isEqualTo(79);
        assertThat(again.newCount()).isZero();
        confirm.confirm(again.id(), false);
        assertThat(entriesIn(OCT)).hasSize(79);
    }

    @Test
    @DisplayName("RF-72: only the latest batch of a card can be undone and undo restores the previous state (Q7)")
    void undo() {
        ImportBatch october = importAndConfirm(Fixtures.CLOSED_PDF, OCT);
        ImportBatch november = importAndConfirm(Fixtures.OPEN_XLSX, NOV);
        assertThatThrownBy(() -> manage.undo(october.id())).isInstanceOf(BusinessRuleException.class)
                .hasMessage("import-not-undoable");
        manage.undo(november.id());
        assertThat(entriesIn(NOV)).allMatch(e -> e.status() == EntryStatus.PROJECTED);
        assertThat(bills.findByCardIdAndBillingPeriod(cardId, NOV).orElseThrow().partial()).isFalse();
        manage.undo(october.id());
        assertThat(entries.findByCardBillCardId(cardId)).isEmpty();
        assertThat(purchases.findByCardId(cardId)).isEmpty();
        assertThat(queries.get(october.id()).status()).isEqualTo(BatchStatus.UNDONE);
    }

    @Test
    @DisplayName("RF-69, RF-68: a preview can be cancelled and divergent lines need a decision")
    void cancelAndDecisions() {
        ImportBatch batch = preview.preview(cardId, OCT, Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        ImportLine first = queries.lines(batch.id()).getFirst();
        assertThatThrownBy(() -> manage.editLine(batch.id(), first.id(),
                com.planejamentomensal.imports.domain.LineDecision.UPDATE_EXISTING, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        manage.editLine(batch.id(), first.id(), com.planejamentomensal.imports.domain.LineDecision.IGNORE, null, null, "Amazon");
        assertThat(manage.cancel(batch.id()).status()).isEqualTo(BatchStatus.CANCELLED);
        assertThatThrownBy(() -> confirm.confirm(batch.id(), false)).isInstanceOf(BusinessRuleException.class);
    }
}

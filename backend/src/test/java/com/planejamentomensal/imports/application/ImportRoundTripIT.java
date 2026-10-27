package com.planejamentomensal.imports.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.CardDetails;
import com.planejamentomensal.entry.application.CardBillRepository;
import com.planejamentomensal.entry.application.ReviewQueue;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.LineStatus;
import com.planejamentomensal.imports.infra.itau.Fixtures;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the import as the app does: every call in its own transaction on a database of its own, so values
 * are really written and read back (no shared persistence context hides storage problems).
 */
@SpringBootTest(properties = "app.home=${java.io.tmpdir}/pm-roundtrip-${random.uuid}")
class ImportRoundTripIT {

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private PreviewImportUseCase preview;
    @Autowired private ConfirmImportUseCase confirm;
    @Autowired private ImportQueries queries;
    @Autowired private CardBillRepository bills;
    @Autowired private ReviewQueue queue;
    @Autowired private TransactionTemplate transactions;

    @Test
    @DisplayName("RF-72: dates, amounts and statuses survive writing and reading back across transactions")
    void roundTrip() {
        long member = members.create(new MemberDetails("RT", "Casa", MemberKind.MAIN, "#000000", null, null, null, "pt-BR")).id();
        long card = cards.create(new CardDetails("Itaú", Bank.ITAU, List.of("4102", "4090"), "MARIANA S F SILVA", null, member), 2, 9).id();
        ImportBatch october = preview.preview(card, BillingPeriod.of(2026, 10), Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        confirm.confirm(october.id(), false);
        ImportBatch november = preview.preview(card, BillingPeriod.of(2026, 11), Fixtures.OPEN_XLSX, Fixtures.bytes(Fixtures.OPEN_XLSX));
        assertThat(november.matchedCount()).isGreaterThanOrEqualTo(10);
        assertThat(queries.lines(november.id())).anyMatch(l -> l.status() == LineStatus.MATCHED
                && l.purchaseDate().equals(LocalDate.of(2026, 9, 20)));
        confirm.confirm(november.id(), false);
        transactions.executeWithoutResult(status -> {
            var bill = bills.findByCardIdAndBillingPeriod(card, BillingPeriod.of(2026, 10)).orElseThrow();
            assertThat(bill.dueDate()).isEqualTo(LocalDate.of(2026, 10, 9));
            assertThat(bill.closingDate()).isEqualTo(LocalDate.of(2026, 9, 28));
        });
        assertThat(queries.get(october.id()).status()).isEqualTo(BatchStatus.CONFIRMED);
        assertThat(queries.get(october.id()).importedAt()).isNotNull();
        assertThat(queue.pending(card, BillingPeriod.of(2026, 10), null)).isNotEmpty()
                .allSatisfy(item -> assertThat(item.purchaseDate()).isBefore(LocalDate.of(2026, 10, 1)));
        long third = members.create(new MemberDetails("RU", "Tia", MemberKind.THIRD_PARTY, "#111111", null, null, null, "pt-BR")).id();
        var item = queue.pending(card, BillingPeriod.of(2026, 10), null).getFirst();
        queue.apply(java.util.List.of(item.purchaseId()), new com.planejamentomensal.entry.application.ReviewEdit(java.util.List.of(
                new com.planejamentomensal.entry.application.ReviewItem.Share(third, new java.math.BigDecimal("33.33"), null),
                new com.planejamentomensal.entry.application.ReviewItem.Share(member, new java.math.BigDecimal("66.67"), null)),
                null, true, null, false), false, null);
        var reread = queue.item(item.purchaseId());
        assertThat(reread.splits()).extracting(s -> s.percentage().stripTrailingZeros().toPlainString())
                .containsExactlyInAnyOrder("33.33", "66.67");
    }
}

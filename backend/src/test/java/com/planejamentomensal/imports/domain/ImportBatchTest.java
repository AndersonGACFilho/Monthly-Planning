package com.planejamentomensal.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportBatchTest {

    private static ParsedBill bill(boolean totalsMatch) {
        RawBillLine line = new RawBillLine(1, RawLineKind.PURCHASE, LocalDate.of(2026, 9, 1), "LOJA", 1, 2,
                Money.of("10.00"), "H", "4102", "outros", null, null);
        return new ParsedBill(BillFormat.PDF, false, BillingPeriod.of(2026, 10), LocalDate.of(2026, 10, 9),
                LocalDate.of(2026, 9, 28), LocalDate.of(2026, 11, 2), "4102", Money.of("10.00"),
                totalsMatch ? Money.of("10.00") : Money.of("11.00"), List.of(), List.of(line), List.of());
    }

    @Test
    @DisplayName("RF-72: a batch moves from preview to confirmed or cancelled, and from confirmed to undone")
    void transitions() {
        ImportBatch batch = new ImportBatch(1, BillingPeriod.of(2026, 10), "f.pdf", "hash", bill(true), List.of());
        assertThat(batch.status()).isEqualTo(BatchStatus.PREVIEW);
        assertThatThrownBy(() -> batch.moveTo(BatchStatus.UNDONE)).isInstanceOf(IllegalStateException.class);
        batch.moveTo(BatchStatus.CONFIRMED);
        assertThatThrownBy(() -> batch.moveTo(BatchStatus.CANCELLED)).isInstanceOf(IllegalStateException.class);
        batch.moveTo(BatchStatus.UNDONE);
        assertThatThrownBy(() -> batch.moveTo(BatchStatus.PREVIEW)).isInstanceOf(IllegalStateException.class);
        assertThat(batch.format()).isEqualTo(BillFormat.PDF);
        assertThat(batch.fileHash()).isEqualTo("hash");
        assertThat(batch.importedAt()).isNotNull();
        assertThat(batch.closingDate()).isEqualTo(LocalDate.of(2026, 9, 28));
    }

    @Test
    @DisplayName("RF-64h, RF-71: totals difference blocks confirmation until acknowledged")
    void totals() {
        ImportBatch batch = new ImportBatch(1, BillingPeriod.of(2026, 10), "f.pdf", "hash", bill(false), List.of("totals-mismatch"));
        assertThat(batch.totalsBlockConfirmation()).isTrue();
        batch.acknowledgeTotals();
        assertThat(batch.totalsBlockConfirmation()).isFalse();
        assertThat(batch.totalsAcknowledged()).isTrue();
        assertThat(batch.warnings()).containsExactly("totals-mismatch");
        assertThat(new ImportBatch(1, BillingPeriod.of(2026, 10), "f", "h", bill(true), List.of()).warnings()).isEmpty();
    }

    @Test
    @DisplayName("RF-69, RF-70: line decisions and edits; only divergent lines update existing entries")
    void lines() {
        RawBillLine raw = bill(true).lines().getFirst();
        ImportLine line = new ImportLine(1, raw, false, 1, "pk", "ik", new Classification(LineStatus.DIVERGENT, 9L), 3L, false);
        assertThat(line.decision()).isEqualTo(LineDecision.UNDECIDED);
        line.decide(LineDecision.UPDATE_EXISTING);
        line.edit(4L, 5L, "  Loja  ");
        assertThat(line.memberId()).isEqualTo(4L);
        assertThat(line.categoryId()).isEqualTo(5L);
        assertThat(line.aliasName()).isEqualTo("Loja");
        line.edit(null, null, " ");
        assertThat(line.aliasName()).isNull();
        assertThat(line.installmentLabel()).isEqualTo("01/02");
        assertThat(line.holderName()).isEqualTo("H");
        assertThat(line.cardLastDigits()).isEqualTo("4102");
        assertThat(line.bankCategory()).isEqualTo("outros");
        ImportLine future = new ImportLine(1, raw, true, 1, "pk", "ik", Classification.newLine(), 3L, true);
        assertThat(future.decision()).isEqualTo(LineDecision.IGNORE);
        assertThat(future.unknownFinal()).isTrue();
        assertThatThrownBy(() -> future.decide(LineDecision.UPDATE_EXISTING)).isInstanceOf(IllegalArgumentException.class);
        ImportBatch batch = new ImportBatch(1, BillingPeriod.of(2026, 10), "f", "h", bill(true), List.of());
        batch.count(List.of(line, future));
        assertThat(batch.divergentCount()).isEqualTo(1);
        assertThat(batch.newCount()).isZero();
    }
}

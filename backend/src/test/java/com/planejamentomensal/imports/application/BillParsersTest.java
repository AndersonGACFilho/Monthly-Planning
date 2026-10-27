package com.planejamentomensal.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.infra.itau.Fixtures;
import com.planejamentomensal.imports.infra.itau.ItauPdfParser;
import com.planejamentomensal.imports.infra.itau.ItauXlsxParser;
import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillParsersTest {

    private final BillParsers parsers = new BillParsers(List.of(new ItauPdfParser(), new ItauXlsxParser()));

    @Test
    @DisplayName("RF-61, SOL-02: the format is detected by extension and routed to its parser")
    void detectsFormat() {
        assertThat(parsers.parse("Fatura.PDF", Fixtures.bytes(Fixtures.CLOSED_PDF)).format()).isEqualTo(BillFormat.PDF);
        assertThat(parsers.parse("aberta.xlsx", Fixtures.bytes(Fixtures.OPEN_XLSX)).format()).isEqualTo(BillFormat.XLSX);
        assertThat(BillFormat.fromFileName(null)).isEmpty();
    }

    @Test
    @DisplayName("RF-61: other extensions and wrong content are rejected before processing")
    void rejectsUnsupported() {
        assertThatThrownBy(() -> parsers.parse("extrato.csv", new byte[0]))
                .isInstanceOf(InvalidInputException.class).hasMessage("unsupported-bill-format");
        assertThatThrownBy(() -> parsers.parse("fatura.pdf", Fixtures.bytes(Fixtures.OPEN_XLSX)))
                .isInstanceOf(InvalidInputException.class).hasMessage("invalid-bill-file");
        assertThatThrownBy(() -> new BillParsers(List.of()).parse("fatura.pdf", new byte[0]))
                .isInstanceOf(InvalidInputException.class);
    }
}

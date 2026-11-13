package com.planejamentomensal.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportFormatTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 13);

    @Test
    @DisplayName("RNF-07, IMP-69: each export format has its media type and file name; the database is dated")
    void formats() {
        assertThat(ExportFormat.of("json").fileName(DAY)).isEqualTo("planejamento-mensal-dados.json");
        assertThat(ExportFormat.of("CSV").mediaType()).isEqualTo("application/zip");
        assertThat(ExportFormat.of("db").fileName(DAY)).isEqualTo("planejamento-mensal-2026-11-13.db");
        assertThat(ExportFormat.of("db").mediaType()).isEqualTo("application/vnd.sqlite3");
    }

    @Test
    @DisplayName("IMP-69: an unknown format is refused instead of falling back to JSON")
    void unknown() {
        assertThatThrownBy(() -> ExportFormat.of("xml"))
                .isInstanceOf(InvalidInputException.class).hasFieldOrPropertyWithValue("code", "export-format-invalid");
    }
}

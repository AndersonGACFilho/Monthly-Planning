package com.planejamentomensal.shared.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FileNameSanitizerTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = ';', value = {
        "Extrato Mãe 11/2026      ; extrato-mae-11-2026",
        "  --Cartões: Itaú*  --   ; cartoes-itau",
        "AB_Casa                  ; ab_casa",
        "CON                      ; con_",
        "lpt1                     ; lpt1_",
        "日本                      ; file",
        "a<b>c:d\"e|f?g*h\\i       ; a-b-c-d-e-f-g-h-i"
    })
    @DisplayName("POR-04: names keep only letters, digits, - and _ in lower case")
    void sanitizesBaseNames(String text, String expected) {
        // Given / When
        String name = FileNameSanitizer.baseName(text);
        // Then
        assertThat(name).isEqualTo(expected).matches("[a-z0-9_-]+");
    }

    @Test
    @DisplayName("POR-04: names differing only in case become equal")
    void caseOnlyDifferencesCollapse() {
        // Given / When / Then
        assertThat(FileNameSanitizer.baseName("Extrato MÃE")).isEqualTo(FileNameSanitizer.baseName("extrato mãe"));
    }

    @Test
    @DisplayName("POR-04: long names are cut without a trailing dash")
    void longNamesAreCut() {
        // Given
        String text = "a".repeat(FileNameSanitizer.MAX_BASE_LENGTH - 1) + " bbbb";
        // When
        String name = FileNameSanitizer.baseName(text);
        // Then
        assertThat(name).hasSizeLessThanOrEqualTo(FileNameSanitizer.MAX_BASE_LENGTH).doesNotEndWith("-");
    }

    @Test
    @DisplayName("POR-04: file name gets a clean lower-case extension")
    void buildsFileNameWithExtension() {
        // Given / When / Then
        assertThat(FileNameSanitizer.fileName("Extrato 2026-11", ".PDF")).isEqualTo("extrato-2026-11.pdf");
        assertThatThrownBy(() -> FileNameSanitizer.fileName("x", "..")).isInstanceOf(IllegalArgumentException.class);
    }
}

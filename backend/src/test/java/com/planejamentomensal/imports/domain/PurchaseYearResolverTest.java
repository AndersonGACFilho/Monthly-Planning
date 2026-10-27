package com.planejamentomensal.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.MonthDay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PurchaseYearResolverTest {

    private static final LocalDate CLOSING = LocalDate.of(2026, 9, 28);

    @ParameterizedTest(name = "{0} installment {1} -> {2}")
    @CsvSource(nullValues = "null", value = {
        "10-14, 12, 2025-10-14",
        "02-12, 8, 2026-02-12",
        "08-28, null, 2026-08-28",
        "09-28, null, 2026-09-28",
        "09-29, null, 2025-09-29",
        "12-20, 1, 2025-12-20",
        "09-12, 1, 2026-09-12"
    })
    @DisplayName("RF-64f: year of a dd/mm date from closing date and installment number")
    void resolves(String monthDay, Integer installment, String expected) {
        LocalDate date = PurchaseYearResolver.resolve(MonthDay.parse("--" + monthDay), CLOSING, installment);
        assertThat(date).isEqualTo(LocalDate.parse(expected));
    }

    @Test
    @DisplayName("RF-64f: 29 February only exists in leap years")
    void leapDay() {
        assertThat(PurchaseYearResolver.resolve(MonthDay.of(2, 29), LocalDate.of(2026, 3, 5), null))
                .isEqualTo(LocalDate.of(2024, 2, 29));
    }
}

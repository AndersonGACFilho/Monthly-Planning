package com.planejamentomensal.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BillingPeriodTest {

    @Test
    @DisplayName("RN-02: billing period is the month of the due date")
    void periodOfDueDate() {
        assertThat(BillingPeriod.ofDueDate(LocalDate.of(2026, 11, 10))).isEqualTo(BillingPeriod.of(2026, 11));
        assertThat(BillingPeriod.of(2026, 11)).hasToString("2026-11");
    }

    @Test
    @DisplayName("IMP-09: next and previous cross the year")
    void yearRollover() {
        assertThat(BillingPeriod.of(2026, 12).next()).isEqualTo(BillingPeriod.of(2027, 1));
        assertThat(BillingPeriod.of(2027, 1).previous()).isEqualTo(BillingPeriod.of(2026, 12));
        assertThat(BillingPeriod.of(2026, 10).plusMonths(14)).isEqualTo(BillingPeriod.of(2027, 12));
        assertThat(BillingPeriod.of(2026, 10).monthsUntil(BillingPeriod.of(2027, 2))).isEqualTo(4);
    }

    @Test
    @DisplayName("IMP-09: day 31 in February is clamped to the month length")
    void clampsDay() {
        assertThat(BillingPeriod.of(2026, 2).atDay(31)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(BillingPeriod.of(2028, 2).atDay(31)).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(BillingPeriod.of(2026, 3).atDay(0)).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    @DisplayName("IMP-09: parse, order and store as yyyy-MM")
    void parseAndStore() {
        BillingPeriod period = BillingPeriod.parse("2026-11");
        assertThat(period.year()).isEqualTo(2026);
        assertThat(period.month()).isEqualTo(11);
        assertThat(period.compareTo(BillingPeriod.of(2026, 12))).isNegative();
        assertThatThrownBy(() -> BillingPeriod.parse("11/2026")).isInstanceOf(IllegalArgumentException.class);
        BillingPeriodConverter converter = new BillingPeriodConverter();
        assertThat(converter.convertToDatabaseColumn(period)).isEqualTo("2026-11");
        assertThat(converter.convertToEntityAttribute("2026-11")).isEqualTo(period);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}

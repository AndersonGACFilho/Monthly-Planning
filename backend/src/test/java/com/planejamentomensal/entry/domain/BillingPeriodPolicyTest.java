package com.planejamentomensal.entry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BillingPeriodPolicyTest {

    @ParameterizedTest(name = "purchase {0}, closing {1}, due {2} -> {3}")
    @CsvSource({
        "2026-10-05, 3, 10, 2026-11",
        "2026-10-03, 3, 10, 2026-10",
        "2026-10-04, 3, 10, 2026-11",
        "2026-12-15, 3, 10, 2027-01",
        "2026-10-10, 28, 5, 2026-11",
        "2026-10-29, 28, 5, 2026-12",
        "2026-02-28, 30, 7, 2026-03",
        "2026-03-01, 30, 7, 2026-04"
    })
    @DisplayName("RN-01, RN-02: purchase up to the closing day goes to that bill, later to the next; period is the due month")
    void firstPeriod(LocalDate purchase, int closingDay, int dueDay, String expected) {
        assertThat(BillingPeriodPolicy.firstPeriod(purchase, closingDay, dueDay, Map.of()))
                .isEqualTo(BillingPeriod.parse(expected));
    }

    @Test
    @DisplayName("RN-22: closing dates read from files win over the card's typical closing day")
    void knownClosingWins() {
        Map<BillingPeriod, LocalDate> known = Map.of(BillingPeriod.of(2026, 11), LocalDate.of(2026, 11, 2));
        assertThat(BillingPeriodPolicy.firstPeriod(LocalDate.of(2026, 11, 2), 3, 10, known)).isEqualTo(BillingPeriod.of(2026, 11));
        assertThat(BillingPeriodPolicy.firstPeriod(LocalDate.of(2026, 11, 3), 3, 10, known)).isEqualTo(BillingPeriod.of(2026, 12));
        assertThat(BillingPeriodPolicy.firstPeriod(LocalDate.of(2026, 10, 5), 3, 10, known)).isEqualTo(BillingPeriod.of(2026, 11));
    }

    @Test
    @DisplayName("RN-02: the due date is the first due day after the closing date")
    void dueAfterClosing() {
        assertThat(BillingPeriodPolicy.dueAfter(LocalDate.of(2026, 11, 3), 10)).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(BillingPeriodPolicy.dueAfter(LocalDate.of(2026, 10, 28), 5)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(BillingPeriodPolicy.dueAfter(LocalDate.of(2026, 12, 28), 5)).isEqualTo(LocalDate.of(2027, 1, 5));
    }
}

package com.planejamentomensal.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PurchaseMatcherTest {

    @ParameterizedTest(name = "{0} {1} {2} vs {3} {4} {5} -> {6}")
    @CsvSource(delimiter = ';', value = {
        "2026-09-20; BMB *Equato; 276.77; 2026-09-20; Bmb    *Equatorialporto Alegrebra; 276.77; true",
        "2026-08-31; MERCADOLIVRE*M; 48.01; 2026-08-30; Mercadolivre*mercadoljaragua Do Sbra; 48.01; true",
        "2026-08-31; MERCADOLIVRE*M; 48.02; 2026-08-31; MERCADOLIVRE*M; 48.01; true",
        "2026-08-31; MERCADOLIVRE*M; 48.04; 2026-08-31; MERCADOLIVRE*M; 48.01; false",
        "2026-08-31; MERCADOLIVRE*M; 48.01; 2026-08-29; MERCADOLIVRE*M; 48.01; false",
        "2026-09-05; nuuvem *N; 135.35; 2026-09-04; Nuuvem      *Nuuvemrio De Janeirbra; 135.35; false"
    })
    @DisplayName("RN-17, RN-18, RN-19: same installment across formats with ±1 day, ±R$ 0,02 and store prefix")
    void sameInstallment(LocalDate d1, String s1, String a1, LocalDate d2, String s2, String a2, boolean expected) {
        PurchaseFacts a = new PurchaseFacts(1, d1, s1, 2, 2, Money.of(a1));
        PurchaseFacts b = new PurchaseFacts(1, d2, s2, 2, 2, Money.of(a2));
        assertThat(PurchaseMatcher.sameInstallment(a, b)).isEqualTo(expected);
    }
}

package com.planejamentomensal.imports.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PurchaseFacts;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImportClassifierTest {

    private static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    private static PurchaseFacts facts(String date, String description, Integer n, Integer count, String amount) {
        return new PurchaseFacts(1, LocalDate.parse(date), description, n, count, Money.of(amount));
    }

    private static ExistingEntry existing(long id, ExistingEntry.State state, PurchaseFacts facts) {
        return new ExistingEntry(id, "key-" + id, state, facts, NOV, false);
    }

    @Test
    @DisplayName("RN-13: confirmed entry with the same key or tolerant match is a duplicate")
    void duplicate() {
        var stored = existing(1, ExistingEntry.State.CONFIRMED, facts("2026-09-20", "BMB *Equato", 2, 2, "276.77"));
        var classifier = new ImportClassifier(List.of(stored));
        var line = facts("2026-09-20", "Bmb    *Equatorialporto Alegrebra", 2, 2, "276.77");
        assertThat(classifier.classify(line, "other", NOV)).isEqualTo(new Classification(LineStatus.DUPLICATE, 1L));
        assertThat(new ImportClassifier(List.of(stored)).classify(facts("2026-01-01", "X", null, null, "1"), "key-1", NOV).status())
                .isEqualTo(LineStatus.DUPLICATE);
    }

    @Test
    @DisplayName("RN-13, RN-06, RN-17, RN-18: projected installment within ±1 day and ±R$ 0,02 becomes matched")
    void matched() {
        var projected = existing(2, ExistingEntry.State.PROJECTED, facts("2026-08-31", "MERCADOLIVRE*M", 2, 2, "48.02"));
        var line = facts("2026-08-30", "Mercadolivre*mercadoljaragua Do Sbra", 2, 2, "48.01");
        assertThat(new ImportClassifier(List.of(projected)).classify(line, "x", NOV))
                .isEqualTo(new Classification(LineStatus.MATCHED, 2L));
    }

    @Test
    @DisplayName("RN-13: same store and installment with another amount is divergent")
    void divergent() {
        var projected = existing(3, ExistingEntry.State.PROJECTED, facts("2026-09-20", "BMB *Equato", 2, 2, "276.77"));
        var line = facts("2026-09-20", "BMB *Equatorial", 2, 2, "300.00");
        assertThat(new ImportClassifier(List.of(projected)).classify(line, "x", NOV).status()).isEqualTo(LineStatus.DIVERGENT);
    }

    @Test
    @DisplayName("RN-13: anything else is new, and each stored entry matches at most one line (RN-25)")
    void newAndUsedOnce() {
        var stored = existing(4, ExistingEntry.State.CONFIRMED, facts("2026-09-03", "PETLOVE SAUD*Petlove SS", null, null, "12.00"));
        var classifier = new ImportClassifier(List.of(stored));
        var line = facts("2026-09-03", "PETLOVE SAUD*Petlove SS", null, null, "12.00");
        assertThat(classifier.classify(line, "a", NOV).status()).isEqualTo(LineStatus.DUPLICATE);
        assertThat(classifier.classify(line, "b", NOV).status()).isEqualTo(LineStatus.NEW);
        assertThat(classifier.classify(facts("2026-09-04", "POSTO", null, null, "50"), "c", NOV)).isEqualTo(Classification.newLine());
    }

    @Test
    @DisplayName("RN-09: a projected subscription charge matches by store and billing period")
    void subscription() {
        var charge = new ExistingEntry(5, "s", ExistingEntry.State.PROJECTED,
                facts("2026-10-27", "MP *PARAMOUNTPLUSOsasco", null, null, "29.66"), NOV, true);
        var line = facts("2026-11-26", "MP *PARAMOUNTPLUSOsasco", null, null, "29.90");
        var classifier = new ImportClassifier(List.of(charge));
        assertThat(classifier.classify(line, "x", NOV)).isEqualTo(new Classification(LineStatus.MATCHED, 5L));
        assertThat(classifier.unmatchedIn(NOV)).isEmpty();
    }

    @Test
    @DisplayName("RN-07: projected entries no line matched are reported")
    void unmatched() {
        var projected = existing(6, ExistingEntry.State.PROJECTED, facts("2026-05-06", "DCoreMoveisGOI", 7, 12, "429.00"));
        var classifier = new ImportClassifier(List.of(projected));
        classifier.classify(facts("2026-09-01", "OTHER", null, null, "1"), "x", NOV);
        assertThat(classifier.unmatchedIn(NOV)).containsExactly(projected);
        assertThat(classifier.unmatchedIn(NOV.next())).isEmpty();
    }
}

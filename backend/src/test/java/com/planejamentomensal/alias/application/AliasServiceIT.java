package com.planejamentomensal.alias.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AliasServiceIT {

    @Autowired
    private AliasService aliases;

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;

    @Test
    @DisplayName("RF-46, RF-47: the alias points to the normalized original, never to the previous alias")
    void renameTwice() {
        AliasView first = aliases.renameAll("SUPERMERC JOAO LTDA", "Mercado", null);
        AliasView second = aliases.renameAll("supermerc joão ltda", "Mercado do bairro", null);
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.displayName()).isEqualTo("Mercado do bairro");
        assertThat(second.patterns()).containsExactly("SUPERMERCJOAOLTDA");
        assertThat(aliases.aliasOf("SUPERMERC  JOAO LTDA")).map(AliasView::displayName).contains("Mercado do bairro");
    }

    @Test
    @DisplayName("RN-24: descriptions of the same store are suggested and grouped into one alias")
    void groups() {
        AliasView paramount = aliases.renameAll("EC *PARAMOUNTPLUSOS", "Paramount+", null);
        assertThat(aliases.sameStoreAlias("EC *PARAMOUNTPLUSOSASCO")).map(AliasView::id).contains(paramount.id());
        aliases.addDescription(paramount.id(), "MP *PARAMOUNTPLUSOsasco");
        assertThat(aliases.aliasOf("MP *PARAMOUNTPLUSOsasco")).map(AliasView::id).contains(paramount.id());
        assertThat(aliases.sameStoreAlias("EC *PARAMOUNTPLUSOS")).isEmpty();
        aliases.restoreOriginal(999L, "EC *PARAMOUNTPLUSOS");
        assertThat(aliases.aliasOf("EC *PARAMOUNTPLUSOS")).isEmpty();
        assertThat(aliases.aliasOf("MP *PARAMOUNTPLUSOsasco")).isPresent();
        aliases.restoreOriginal(999L, "MP *PARAMOUNTPLUSOsasco");
        assertThat(aliases.all()).doesNotContainKey(paramount.id());
    }

    @Test
    @DisplayName("RF-48, RF-51: one entry can have its own name, and search finds aliases by name or original")
    void overridesAndSearch() {
        LedgerFixture f = new LedgerFixture(members, cards, "6");
        long one = ledger.recordPurchase(null, f.purchase("POSTO AGRONOMIAPORTO AL", "50.00", LocalDate.of(2026, 9, 3), BillingPeriod.of(2026, 10), null, null, "PURCHASE"));
        long two = ledger.recordPurchase(null, f.purchase("POSTO AGRONOMIAPORTO AL", "60.00", LocalDate.of(2026, 9, 4), BillingPeriod.of(2026, 10), null, null, "PURCHASE"));
        aliases.renameAll("POSTO AGRONOMIAPORTO AL", "Posto do bairro", null);
        aliases.renameOne(one, "Gasolina da viagem");
        Map<Long, AliasService.Display> display = aliases.display(Map.of(one, "POSTO AGRONOMIAPORTO AL", two, "POSTO AGRONOMIAPORTO AL"));
        assertThat(display.get(one).text()).isEqualTo("Gasolina da viagem");
        assertThat(display.get(two).text()).isEqualTo("Posto do bairro");
        assertThat(display.get(two).renamed()).isTrue();
        assertThat(aliases.overridesOf(java.util.List.of(one))).containsEntry(one, "Gasolina da viagem");
        aliases.renameOne(one, "Gasolina");
        aliases.restoreOriginal(one, "POSTO AGRONOMIAPORTO AL");
        assertThat(aliases.display(Map.of(one, "POSTO AGRONOMIAPORTO AL")).get(one).text()).isEqualTo("Posto do bairro");
        assertThat(aliases.search("bairro")).hasSize(1);
        assertThat(aliases.search("agronomia")).hasSize(1);
        assertThat(aliases.search("inexistente")).isEmpty();
    }
}

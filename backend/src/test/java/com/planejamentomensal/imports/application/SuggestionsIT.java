package com.planejamentomensal.imports.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.category.application.SaveCategoryUseCase;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewQueue;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.infra.itau.Fixtures;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SuggestionsIT {

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private SaveCategoryUseCase categories;
    @Autowired private AliasService aliases;
    @Autowired private PreviewImportUseCase preview;
    @Autowired private ConfirmImportUseCase confirm;
    @Autowired private ManageImportUseCase manage;
    @Autowired private ImportQueries queries;
    @Autowired private ReviewQueue queue;
    @Autowired private Settings settings;

    @Test
    @DisplayName("RF-70, RF-49, RF-79d: preview applies alias and its category, confirmation stores new aliases and auto-validates")
    void aliasesInPreview() {
        LedgerFixture f = new LedgerFixture(members, cards, "4");
        long fuel = categories.create("Combustível", null, "#000000", "fuel").id();
        aliases.renameAll("POSTO AGRONOMIAPORTO AL", "Posto do bairro", fuel);
        settings.change(Map.of("auto-validate", "true"));
        var batch = preview.preview(f.card, BillingPeriod.of(2026, 10), Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        ImportLine posto = queries.lines(batch.id()).stream().filter(l -> l.description().equals("POSTO AGRONOMIAPORTO AL")).findFirst().orElseThrow();
        assertThat(posto.aliasName()).isEqualTo("Posto do bairro");
        assertThat(posto.categoryId()).isEqualTo(fuel);
        ImportLine kindle = queries.lines(batch.id()).stream().filter(l -> l.description().startsWith("Amazon Kindle")).findFirst().orElseThrow();
        manage.editLine(batch.id(), kindle.id(), null, null, null, "Kindle");
        confirm.confirm(batch.id(), false);
        assertThat(aliases.aliasOf("Amazon Kindle UnltdSAO")).map(a -> a.displayName()).contains("Kindle");
        assertThat(queue.pending(f.card, null, null)).noneMatch(i -> i.originalDescription().equals("POSTO AGRONOMIAPORTO AL"));
        assertThat(queue.pending(f.card, null, null)).anyMatch(i -> i.displayDescription().equals("Kindle"));
    }

    @Test
    @DisplayName("RF-08, RF-75: the category chosen for a description is suggested on the next entries")
    void lastCategoryIsSuggested() {
        LedgerFixture f = new LedgerFixture(members, cards, "5");
        long pets = categories.create("Pets", null, "#000000", "paw-print").id();
        var october = preview.preview(f.card, BillingPeriod.of(2026, 10), Fixtures.CLOSED_PDF, Fixtures.bytes(Fixtures.CLOSED_PDF));
        confirm.confirm(october.id(), false);
        var petlove = queue.pending(f.card, null, null).stream().filter(i -> i.originalDescription().startsWith("PETLOVE")).toList();
        queue.apply(java.util.List.of(petlove.getFirst().purchaseId()),
                new com.planejamentomensal.entry.application.ReviewEdit(null, pets, false, null, false), true, null);
        assertThat(queue.item(petlove.get(1).purchaseId()).suggestedCategoryId()).isEqualTo(pets);
    }
}

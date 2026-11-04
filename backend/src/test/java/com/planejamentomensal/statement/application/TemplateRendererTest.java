package com.planejamentomensal.statement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.StatementBlock;
import com.planejamentomensal.statement.domain.StatementColumn;
import com.planejamentomensal.statement.domain.StatementCustomization;
import com.planejamentomensal.statement.domain.TemplateSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TemplateRendererTest {

    @Autowired private TemplateRenderer renderer;
    @Autowired private TemplateService templates;
    @Autowired private TemplateLayoutGuide reference;

    /**
     * Compares the rendered sample statement with the stored snapshot; run with {@code -Dsnapshot.update=true} to
     * accept a deliberate change.
     */
    @ParameterizedTest
    @ValueSource(strings = {"pt-BR", "en-US"})
    @DisplayName("I18N-14, I18N-10, TPL-01: the statement HTML matches its snapshot in each language")
    void snapshot(String language) throws Exception {
        MemberStatement sample = SampleStatement.of(BillingPeriod.of(2026, 11));
        String html = renderer.render(sample, templates.defaultSettings(), language, null).html()
                .replaceAll("\\d{2}/\\d{2}/\\d{4}(?=</span>\\s*</div>)", "<issue-date>");
        Path file = Path.of("src/test/resources/snapshots/statement-" + language + ".html");
        if (Boolean.getBoolean("snapshot.update") || Files.notExists(file)) {
            Files.writeString(file, html, StandardCharsets.UTF_8);
        }
        assertThat(html).isEqualTo(Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("TPL-07: a full layout replaces the body, keeps the styles and shows the chosen blocks it places")
    void fullLayout() {
        TemplateSettings base = templates.defaultSettings();
        TemplateSettings layout = new TemplateSettings(base.primaryColor(), base.secondaryColor(), base.font(), null,
                List.of(StatementBlock.SUMMARY, StatementBlock.COMMITTED), base.texts(), ".capa { color: red; }", null,
                "<div class=\"capa\">{{member.name}} · {{total}}</div>{{block.committed}}<script>x()</script>");
        String html = renderer.render(SampleStatement.of(BillingPeriod.of(2026, 11)), layout, "pt-BR", null).html();
        Document page = Jsoup.parse(html);
        assertThat(page.select("style").html()).contains(".capa { color: red; }", "table.lines");
        assertThat(page.body().child(0).text()).isEqualTo("Maria · R$ 465,20");
        assertThat(page.getElementById("block-committed")).isNotNull();
        assertThat(page.getElementById("block-summary")).isNull();
        assertThat(page.getElementById("statement-header")).isNull();
        assertThat(html).doesNotContain("<script>");
    }

    @Test
    @DisplayName("TPL-07: every class and id of the statement is listed with a description in both languages")
    void layoutReference() {
        for (Locale locale : List.of(Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("en-US"))) {
            TemplateLayoutGuide.LayoutReference ref = reference.reference(locale);
            assertThat(ref.selectors()).extracting(TemplateLayoutGuide.LayoutSelector::selector)
                    .contains("#statement-header", "#block-cards", "#statement-footer", ".statement", ".card-group", ".pending");
            assertThat(ref.selectors()).allSatisfy(s -> assertThat(s.description()).as(s.selector()).isNotBlank());
        }
    }

    @Test
    @DisplayName("IMP-60: one export changes header, footer, blocks and columns without touching the template")
    void customization() {
        TemplateSettings base = templates.defaultSettings();
        StatementCustomization custom = new StatementCustomization("Cobrança de {{member.name}}", "Obrigado!",
                List.of(StatementBlock.CARDS, StatementBlock.SUMMARY),
                java.util.Set.of(StatementColumn.DATE, StatementColumn.SHARE));
        Document page = Jsoup.parse(renderer.render(SampleStatement.of(BillingPeriod.of(2026, 11)), base, "en-US", null,
                custom).html());
        assertThat(page.selectFirst("h1").text()).isEqualTo("Cobrança de Maria");
        assertThat(page.getElementById("statement-footer").text()).isEqualTo("Obrigado!");
        assertThat(page.select(".block").eachAttr("id")).containsExactly("block-cards", "block-summary");
        assertThat(page.select("#block-cards thead th").eachText()).containsExactly("Description", "Installment", "Category", "Amount");
        assertThat(page.selectFirst("#block-cards tr.subtotal td").attr("colspan")).isEqualTo("3");
        assertThat(base.blocks()).hasSize(StatementBlock.values().length);
    }
}

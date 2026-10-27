package com.planejamentomensal.statement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.statement.application.TemplateSanitizer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateRulesTest {

    private static final TemplateTexts PT = new TemplateTexts("Extrato de {{member.name}}", "Rodapé", "Olá", "Assunto {{total}}",
            "Corpo", "Zap {{pixKey}}");

    private static TemplateSettings settings(Map<String, TemplateTexts> texts, String css, String html) {
        return new TemplateSettings("#2563EB", "#475569", TemplateFont.NOTO_SANS, null, List.of(StatementBlock.SUMMARY,
                StatementBlock.CARDS), texts, css, html);
    }

    @Test
    @DisplayName("TPL-05: English variables are replaced; unknown ones are rejected")
    void variables() {
        assertThat(MessageVariables.apply("{{member.name}} deve {{ total }} até {{dueDate}}",
                Map.of("member.name", "Ana", "total", "R$ 10,00", "dueDate", "10/11/2026"))).isEqualTo("Ana deve R$ 10,00 até 10/11/2026");
        assertThat(MessageVariables.unknown("{{member.email}} {{total}} {{foo}}")).containsExactly("member.email", "foo");
        assertThat(MessageVariables.apply(null, Map.of())).isEmpty();
        assertThatThrownBy(() -> settings(Map.of("pt-BR", new TemplateTexts("{{senha}}", null, null, null, null, null)), null, null).validate())
                .isInstanceOf(InvalidInputException.class).hasFieldOrPropertyWithValue("code", "template-variable-unknown");
    }

    @Test
    @DisplayName("TPL-02: colours, font, logo, blocks and languages are validated")
    void validation() {
        settings(Map.of("pt-BR", PT), null, null).validate();
        assertThatThrownBy(() -> new TemplateSettings("blue", "#475569", TemplateFont.NOTO_SANS, null, List.of(StatementBlock.CARDS),
                Map.of(), null, null).validate()).hasFieldOrPropertyWithValue("code", "template-invalid");
        assertThatThrownBy(() -> new TemplateSettings("#2563EB", "#475569", TemplateFont.NOTO_SANS, "http://x/logo.png",
                List.of(StatementBlock.CARDS), Map.of(), null, null).validate()).hasMessageContaining("template-invalid");
        assertThatThrownBy(() -> new TemplateSettings("#2563EB", "#475569", TemplateFont.NOTO_SANS, null,
                List.of(StatementBlock.CARDS, StatementBlock.CARDS), Map.of(), null, null).validate()).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> settings(Map.of("fr-FR", PT), null, null).validate()).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new TemplateSettings("#2563EB", "#475569", null, null, List.of(StatementBlock.CARDS), Map.of(),
                null, null).validate()).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("I18N-11: missing texts in a language fall back to the other one with a warning flag")
    void fallback() {
        TemplateSettings s = settings(Map.of("pt-BR", PT, "en-US", new TemplateTexts("", null, null, null, null, null)), null, null);
        assertThat(s.textsFor("pt-BR").fallback()).isFalse();
        assertThat(s.textsFor("en-US").fallback()).isTrue();
        assertThat(s.textsFor("en-US").texts()).isEqualTo(PT);
        assertThat(settings(Map.of(), null, null).textsFor("pt-BR").texts().isEmpty()).isTrue();
    }

    @Test
    @DisplayName("TPL-07: advanced HTML and CSS lose scripts, handlers and external resources")
    void sanitizer() {
        assertThat(TemplateSanitizer.html("<h1 onclick=\"x()\">Oi {{member.name}}</h1><script>alert(1)</script><img src=x>"))
                .isEqualTo("<h1>Oi {{member.name}}</h1>");
        assertThat(TemplateSanitizer.html("<p style=\"background:url(http://evil)\">x</p>")).doesNotContain("evil");
        assertThat(TemplateSanitizer.html(null)).isEmpty();
        assertThat(TemplateSanitizer.css("h1 { color: red; }")).isEqualTo("h1 { color: red; }");
        assertThat(TemplateSanitizer.css("@import url(http://x.css);")).isEmpty();
        assertThat(TemplateSanitizer.css("body { background: url(http://tracker) }")).isEmpty();
        assertThat(TemplateSanitizer.css("body { background: url(data:image/png;base64,AAA) }")).contains("data:image");
        assertThat(TemplateSanitizer.css("</style><script>")).isEmpty();
    }

    @Test
    @DisplayName("TPL-06, SOL-03: a delivery awaiting the user can be marked as sent once")
    void delivery() {
        StatementDelivery d = new StatementDelivery(1, com.planejamentomensal.shared.domain.BillingPeriod.of(2026, 11),
                DeliveryChannelType.WHATSAPP, DeliveryStatus.AWAITING_USER, java.time.Instant.EPOCH, 2, 3, "pt-BR", "abc",
                StatementOptions.DEFAULT, "file.png");
        d.markSent();
        assertThat(d.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(d.options()).isEqualTo(StatementOptions.DEFAULT);
        assertThatThrownBy(d::markSent).isInstanceOf(IllegalStateException.class);
        Template t = new Template("A", "{}", java.time.Instant.EPOCH);
        assertThat(t.change("B", "{}", java.time.Instant.EPOCH)).isFalse();
        assertThat(t.change("B", "{\"x\":1}", java.time.Instant.EPOCH)).isTrue();
        assertThat(t.currentVersion()).isEqualTo(2);
        assertThat(TemplateFont.NOTO_SERIF.family()).isEqualTo("Noto Serif");
    }
}

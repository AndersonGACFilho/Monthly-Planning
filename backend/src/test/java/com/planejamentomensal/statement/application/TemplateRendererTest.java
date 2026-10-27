package com.planejamentomensal.statement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.MemberStatement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TemplateRendererTest {

    @Autowired private TemplateRenderer renderer;
    @Autowired private TemplateService templates;

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
}

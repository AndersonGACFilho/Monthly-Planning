package com.planejamentomensal.statement.application.workbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class WorkbookTemplateTest {

    private static final Path FIXTURE = Path.of("src/test/resources/fixtures/itau/monthly-workbook-model-2026-11.xlsx");
    private static final Path RESOURCE = Path.of("src/main/resources/workbook/monthly-workbook-model.xlsx");

    static WorkbookLabels labels(String language) {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("i18n/messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        return new WorkbookLabels(messages, Locale.forLanguageTag(language));
    }

    /**
     * Rebuilds the bundled model from the masked fixture when run with {@code -Dworkbook.model.update=true} (Q10):
     * every sheet keeps its look and one empty styled row per table, and no data.
     */
    @Test
    @DisplayName("XLS-13: the bundled model is the masked model without data rows")
    void bundledModel() throws Exception {
        if (Boolean.getBoolean("workbook.model.update")) {
            try (XSSFWorkbook workbook = WorkbookTemplate.open(Files.readAllBytes(FIXTURE));
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                WorkbookLabels pt = labels("pt-BR");
                WorkbookData empty = new WorkbookData(BillingPeriod.of(2026, 1), List.of(), List.of(), List.of(), List.of(),
                        List.of(), List.of());
                WorkbookContext ctx = new WorkbookContext(workbook, empty, pt, new CompatibleDialect());
                new ListsSheetWriter().write(ctx);
                new CardsSheetWriter().write(ctx);
                new IncomeSheetWriter().write(ctx);
                new ExpensesSheetWriter().write(ctx);
                new SummarySheetWriter().write(ctx);
                MemberSheetWriter.trimTemplate(workbook.getSheetAt(MemberSheetWriter.templateIndex(workbook, ctx)), ctx);
                workbook.write(out);
                Files.write(RESOURCE, SharedStringsCompactor.compact(out.toByteArray()));
            }
        }
        byte[] model = Files.readAllBytes(RESOURCE);
        try (XSSFWorkbook workbook = WorkbookTemplate.open(model)) {
            assertThat(WorkbookTemplate.missing(workbook)).isEmpty();
            assertThat(workbook.getSheet(ModelSheets.CARDS).getLastRowNum()).isLessThanOrEqualTo(6);
            assertThat(workbook.getNumberOfSheets()).isEqualTo(6);
        }
        String text = allText(model);
        for (String fixtureWord : List.of("Nubank", "Carrefour", "Geladeira", "Empréstimo", "Viagem")) {
            assertThat(text).as(fixtureWord).doesNotContain(fixtureWord);
        }
        assertThat(text).doesNotContain("pivotCache").doesNotContain("DataMashup");
    }

    @Test
    @DisplayName("XLS-12: detail sheets, broken names, the stale pivot table and formulas are removed when the model opens")
    void repairsTheModel() throws Exception {
        try (XSSFWorkbook workbook = WorkbookTemplate.open(Files.readAllBytes(FIXTURE))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                assertThat(workbook.getSheetName(i)).doesNotStartWith("Detalhes");
            }
            assertThat(workbook.getAllNames()).extracting(XSSFName::getRefersToFormula).noneMatch(f -> f.contains("#REF!"));
            assertThat(workbook.getPivotTables()).isEmpty();
            assertThat(workbook.getSheet(ModelSheets.SUMMARY).getRow(4).getCell(1).getCellFormula()).isNull();
        } catch (IllegalStateException expected) {
            // a blank cell has no formula
        }
    }

    @Test
    @DisplayName("XLS-14: a replaced model without the named sheets and tables is refused with what is missing")
    void validates() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook other = new XSSFWorkbook()) {
            other.createSheet("Resumo");
            other.write(out);
        }
        assertThatThrownBy(() -> WorkbookTemplate.open(out.toByteArray()))
                .isInstanceOf(BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("code", "workbook-model-invalid")
                .satisfies(e -> assertThat(((BusinessRuleException) e).arguments().getFirst().toString())
                        .contains("Renda mensal", "Cartões", "CARTÕES", "Categorias"));
    }

    private static String allText(byte[] xlsx) throws Exception {
        StringBuilder text = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                text.append(entry.getName()).append('\n');
                text.append(new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return text.toString();
    }

    static InputStream resource() throws Exception {
        return Files.newInputStream(RESOURCE);
    }
}

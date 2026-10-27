package com.planejamentomensal.statement.application.workbook;

import static com.planejamentomensal.statement.application.StatementFixture.NOV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.statement.application.StatementFixture;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MonthlyWorkbookExporterTest {

    @Autowired private SaveMemberUseCase members;
    @Autowired private SaveCardUseCase cards;
    @Autowired private EntryLedger ledger;
    @Autowired private EntryCommands entries;
    @Autowired private RecurrenceCommands recurrence;
    @Autowired private VariableExpenseCommands expenses;
    @Autowired private Settings settings;
    @Autowired private MonthlyWorkbookExporter exporter;

    @TempDir
    Path dir;

    private StatementFixture s;

    @BeforeEach
    void setUp() {
        s = new StatementFixture(members, cards, ledger, entries, recurrence, expenses, "1");
        recurrence.create(RuleKind.INCOME, new RuleDetails("Salário", s.f.main, null, IncomeType.SALARY, Money.of("5000.00"),
                Recurrence.MONTHLY, 1, 5, NOV, null, null));
        settings.change(Map.of("workbook-suffix", "CJ", "workbook-member-sheets", String.valueOf(s.f.thirdParty)));
    }

    private static XSSFWorkbook open(ExportedWorkbook file) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(file.content()));
    }

    private static List<String> column(XSSFSheet sheet, int col, int from, int to) {
        List<String> values = new ArrayList<>();
        for (int r = from; r <= to; r++) {
            XSSFRow row = sheet.getRow(r);
            XSSFCell cell = row == null ? null : row.getCell(col);
            values.add(cell == null ? "" : cell.getCellType() == CellType.NUMERIC ? String.valueOf(cell.getNumericCellValue())
                    : cell.getCellType() == CellType.FORMULA ? "=" + cell.getCellFormula() : cell.getStringCellValue());
        }
        return values;
    }

    @Test
    @DisplayName("XLS-01, XLS-02, XLS-03, XLS-04, XLS-05, XLS-07, XLS-08: compatible file with grouped card rows, bills group, member totals and named totals")
    void compatible() throws Exception {
        ExportedWorkbook file = exporter.export(NOV, FormulaMode.COMPATIBLE, "pt-BR");
        assertThat(file.fileName()).isEqualTo("2026 - 11 - NOVEMBRO CJ.xlsx");
        assertThat(file.pendingCount()).isGreaterThan(0);
        try (XSSFWorkbook book = open(file)) {
            List<String> names = new ArrayList<>();
            book.forEach(sheet -> names.add(sheet.getSheetName()));
            assertThat(names).containsExactly("Resumo", "Renda mensal", "Despesas mensais", "Cartões", "TIA", "Listas");
            XSSFSheet cardsSheet = book.getSheet("Cartões");
            assertThat(column(cardsSheet, 4, 4, 9)).containsExactly("MERCADO BOM", "MERCADO BOM", "FARMACIA", "POSTO", "", "Plano de saúde");
            assertThat(column(cardsSheet, 2, 4, 5)).containsExactly("M1", "T1");
            assertThat(column(cardsSheet, 0, 9, 9)).containsExactly("Boletos");
            assertThat(column(cardsSheet, 3, 9, 9)).containsExactly("*");
            assertThat(column(cardsSheet, 5, 4, 5)).containsExactly("50.0", "50.0");
            assertThat(column(cardsSheet, 6, 5, 5)).containsExactly("");
            assertThat(cardsSheet.getRow(4).getCell(0).getCellStyle().getFillPattern()).isEqualTo(FillPatternType.SOLID_FOREGROUND);
            assertThat(cardsSheet.getRow(2).getCell(5).getCellFormula()).isEqualTo("SUM(F5:F11)");
            assertThat(cardsSheet.getRow(3).getCell(10).getCellFormula()).isEqualTo("SUMIF($C:$C,K3,$F:$F)");
            assertThat(book.getTable("CARTÕES").getArea().formatAsString()).isEqualTo("A4:G11");
            XSSFSheet income = book.getSheet("Renda mensal");
            assertThat(column(income, 1, 3, 4)).containsExactly("Salário", "Reembolso T1 — Tia");
            assertThat(income.getRow(4).getCell(2).getCellFormula()).isEqualTo("H4");
            assertThat(income.getRow(3).getCell(7).getCellFormula()).isEqualTo("'Cartões'!$K$4");
            XSSFSheet expensesSheet = book.getSheet("Despesas mensais");
            assertThat(column(expensesSheet, 4, 3, 3)).containsExactly("Card 1");
            assertThat(expensesSheet.getRow(3).getCell(5).getCellFormula()).isEqualTo("SUMIF('Cartões'!$A:$A,E4,'Cartões'!$F:$F)");
            assertThat(book.getName("TotalMensaldeRenda").getRefersToFormula()).isEqualTo("SUM('Renda mensal'!$C$4:$C$5)");
            XSSFSheet summary = book.getSheet("Resumo");
            assertThat(summary.getRow(1).getCell(1).getStringCellValue()).isEqualTo("ORÇAMENTO DE NOVEMBRO");
            assertThat(summary.getRow(0).getCell(7).getStringCellValue()).isEqualTo("M1");
            assertThat(summary.getRow(4).getCell(7).getCellFormula()).isEqualTo("SUM(H4:H4)");
            assertThat(book.getPivotTables()).isEmpty();
            XSSFSheet member = book.getSheet("TIA");
            assertThat(member.getRow(1).getCell(1).getStringCellValue()).isEqualTo("SETEMBRO 2026");
            assertThat(column(member, 10, 7, 9)).contains("FARMACIA");
        }
        Files.write(dir.resolve("compatible.xlsx"), file.content());
    }

    @Test
    @DisplayName("XLS-09, XLS-10: Excel 365 mode uses dynamic arrays, structured names and a pivot table refreshed on open")
    void excel365() throws Exception {
        ExportedWorkbook file = exporter.export(NOV, FormulaMode.EXCEL_365, "pt-BR");
        try (XSSFWorkbook book = open(file)) {
            XSSFCell cards = book.getSheet("Despesas mensais").getRow(3).getCell(4);
            assertThat(cards.getCellFormula()).startsWith("_xlfn._xlws.SORT(_xlfn.UNIQUE(_xlfn._xlws.FILTER(CARTÕES[Cartão]");
            assertThat(cards.getCTCell().getCm()).isEqualTo(1);
            assertThat(book.getName("TotalMensaldeRenda").getRefersToFormula()).isEqualTo("SUM(Receita[Valor])");
            assertThat(book.getPivotTables()).hasSize(1);
            var pivot = book.getPivotTables().getFirst();
            assertThat(pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().getRefreshOnLoad()).isTrue();
            assertThat(pivot.getCTPivotTableDefinition().getPageFields().getCount()).isEqualTo(1);
            assertThat(pivot.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(6).getSortType().toString())
                    .isEqualTo("descending");
        }
    }

    @Test
    @DisplayName("XLS-15: en-US translates sheet names, titles, headers and months and keeps the table names")
    void english() throws Exception {
        settings.change(Map.of("workbook-suffix", ""));
        ExportedWorkbook file = exporter.export(NOV, FormulaMode.COMPATIBLE, "en-US");
        assertThat(file.fileName()).isEqualTo("2026 - 11 - NOVEMBER.xlsx");
        try (XSSFWorkbook book = open(file)) {
            assertThat(book.getSheet("Cards")).isNotNull();
            assertThat(book.getSheet("Summary").getRow(1).getCell(1).getStringCellValue()).isEqualTo("BUDGET FOR NOVEMBER");
            assertThat(book.getTable("CARTÕES").getColumns().get(5).getName()).isEqualTo("Amount");
            assertThat(book.getSheet("Monthly income").getRow(4).getCell(1).getStringCellValue()).startsWith("Reimbursement");
            assertThat(book.getSheet("TIA").getRow(1).getCell(1).getStringCellValue()).isEqualTo("SEPTEMBER 2026");
        }
    }

    @Test
    @DisplayName("XLS-18: LibreOffice recalculates the compatible file to the app totals")
    void libreOffice() throws Exception {
        String soffice = List.of("/usr/bin/soffice", "/usr/local/bin/soffice", "C:\\Program Files\\LibreOffice\\program\\soffice.exe")
                .stream().filter(p -> new File(p).canExecute()).findFirst().orElse(null);
        assumeTrue(soffice != null, "LibreOffice is not installed");
        ExportedWorkbook file = exporter.export(NOV, FormulaMode.COMPATIBLE, "pt-BR");
        Path in = Files.createDirectories(dir.resolve("in")).resolve("book.xlsx");
        Files.write(in, file.content());
        Path out = Files.createDirectories(dir.resolve("out"));
        Process process = new ProcessBuilder(soffice, "-env:UserInstallation=file://" + dir.resolve("profile").toAbsolutePath(),
                "--headless", "--convert-to", "xlsx", "--outdir", out.toString(), in.toString()).redirectErrorStream(true).start();
        assertThat(process.waitFor(120, TimeUnit.SECONDS)).isTrue();
        try (XSSFWorkbook book = new XSSFWorkbook(Files.newInputStream(out.resolve("book.xlsx")))) {
            XSSFSheet summary = book.getSheet("Resumo");
            double income = summary.getRow(7).getCell(2).getNumericCellValue();
            double cardsTotal = summary.getRow(9).getCell(2).getNumericCellValue();
            double balance = summary.getRow(11).getCell(2).getNumericCellValue();
            assertThat(cardsTotal).isEqualTo(455.5 + 50 + 200);
            assertThat(income).isEqualTo(5000 + 455.5);
            assertThat(balance).isEqualTo(income - 0 - cardsTotal);
            assertThat(book.getSheet("Cartões").getRow(3).getCell(10).getNumericCellValue()).isEqualTo(455.5);
        }
    }
}

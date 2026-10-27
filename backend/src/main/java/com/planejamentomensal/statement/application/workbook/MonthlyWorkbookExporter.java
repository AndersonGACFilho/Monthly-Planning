package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xssf.usermodel.XSSFName;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the monthly spreadsheet of a billing period from the model: gathers the data, renames the sheets for the
 * language, lets each {@link SheetWriter} fill its sheet and writes the defined total names (XLS-01, XLS-08,
 * XLS-15, XLS-16).
 *
 * @since 1.0
 */
@Service
@Transactional
public class MonthlyWorkbookExporter {

    private static final List<SheetWriter> WRITERS = List.of(new ListsSheetWriter(), new CardsSheetWriter(),
            new IncomeSheetWriter(), new ExpensesSheetWriter(), new MemberSheetWriter(), new SummarySheetWriter());

    private final WorkbookTemplate template;
    private final WorkbookDataCollector collector;
    private final Settings settings;
    private final MessageSource messages;

    /**
     * Creates the exporter.
     *
     * @param template  spreadsheet model
     * @param collector data collector
     * @param settings  language, formula mode and file suffix
     * @param messages  labels
     */
    public MonthlyWorkbookExporter(WorkbookTemplate template, WorkbookDataCollector collector, Settings settings,
            MessageSource messages) {
        this.template = template;
        this.collector = collector;
        this.settings = settings;
        this.messages = messages;
    }

    /**
     * Returns the formula mode used when none is asked: the setting, else the one of this system (XLS-09).
     *
     * @return formula mode
     */
    public FormulaMode defaultMode() {
        String setting = settings.get(Setting.WORKBOOK_FORMULA_MODE);
        return setting.isEmpty() ? FormulaMode.defaultFor(System.getProperty("os.name")) : FormulaMode.valueOf(setting);
    }

    /**
     * Exports a billing period.
     *
     * @param period   billing period
     * @param mode     formula mode, or {@code null} for {@link #defaultMode()}
     * @param language {@code pt-BR} or {@code en-US}, or {@code null} for the app language
     * @return the spreadsheet
     */
    public ExportedWorkbook export(BillingPeriod period, FormulaMode mode, String language) {
        FormulaMode chosen = mode != null ? mode : defaultMode();
        String lang = language != null ? language : settings.get(Setting.LANGUAGE).isEmpty() ? "pt-BR" : settings.get(Setting.LANGUAGE);
        WorkbookLabels labels = new WorkbookLabels(messages, Locale.forLanguageTag("en-US".equals(lang) ? "en-US" : "pt-BR"));
        WorkbookData data = collector.collect(period, labels);
        try (XSSFWorkbook workbook = template.open(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fill(workbook, data, labels, chosen);
            workbook.write(out);
            int pending = (int) data.cardRows().stream().filter(WorkbookData.CardRow::pending).count();
            return new ExportedWorkbook(fileName(period, labels), SharedStringsCompactor.compact(out.toByteArray()), chosen, pending);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Fills an opened model.
     *
     * @param workbook model, repaired
     * @param data     data
     * @param labels   labels
     * @param mode     formula mode
     */
    static void fill(XSSFWorkbook workbook, WorkbookData data, WorkbookLabels labels, FormulaMode mode) {
        rename(workbook, ModelSheets.SUMMARY, labels.sheet("summary"));
        rename(workbook, ModelSheets.INCOME, labels.sheet("income"));
        rename(workbook, ModelSheets.EXPENSES, labels.sheet("expenses"));
        rename(workbook, ModelSheets.CARDS, labels.sheet("cards"));
        rename(workbook, ModelSheets.LISTS, labels.sheet("lists"));
        FormulaDialect dialect = mode == FormulaMode.EXCEL_365 ? new Excel365Dialect() : new CompatibleDialect();
        WorkbookContext ctx = new WorkbookContext(workbook, data, labels, dialect);
        WRITERS.forEach(writer -> writer.write(ctx));
        defineTotal(workbook, ModelSheets.INCOME_NAME, dialect.totalName(ctx, ModelSheets.INCOME_TABLE));
        defineTotal(workbook, ModelSheets.EXPENSES_NAME, dialect.totalName(ctx, ModelSheets.EXPENSES_TABLE));
        workbook.setActiveSheet(workbook.getSheetIndex(labels.sheet("summary")));
        workbook.setForceFormulaRecalculation(true);
    }

    /**
     * Builds the file name {@code AAAA - MM - <MÊS> <suffix>.xlsx} (XLS-01).
     *
     * @param period billing period
     * @param labels labels of the language
     * @return file name
     */
    String fileName(BillingPeriod period, WorkbookLabels labels) {
        String suffix = settings.get(Setting.WORKBOOK_SUFFIX).replaceAll("[\\\\/:*?\"<>|]", "").strip();
        return String.format("%d - %02d - %s%s.xlsx", period.year(), period.month(), labels.month(period),
                suffix.isEmpty() ? "" : " " + suffix);
    }

    private static void rename(XSSFWorkbook workbook, String from, String to) {
        int index = workbook.getSheetIndex(from);
        if (index >= 0 && !from.equals(to)) {
            workbook.setSheetName(index, to);
        }
    }

    private static void defineTotal(XSSFWorkbook workbook, String name, String formula) {
        for (XSSFName existing : List.copyOf(workbook.getAllNames())) {
            if (existing.getNameName().equals(name)) {
                workbook.removeName(existing);
            }
        }
        XSSFName defined = workbook.createName();
        defined.setNameName(name);
        defined.setRefersToFormula(formula);
    }
}

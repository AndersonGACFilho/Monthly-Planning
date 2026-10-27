package com.planejamentomensal.statement.application.workbook;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

/**
 * Summary sheet: title and year from the billing period, % of income spent, totals and balance as formulas, and
 * the expense by category written by the formula dialect (XLS-08, XLS-09, XLS-12).
 *
 * @since 1.0
 */
public class SummarySheetWriter implements SheetWriter {

    private static final int CATEGORY_ROWS = 60;

    /**
     * Creates the writer.
     */
    public SummarySheetWriter() {
        // stateless
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFSheet sheet = ctx.sheet("summary");
        CellStyle[] categoryStyles = {Cells.style(sheet, 0, 6), Cells.style(sheet, 0, 7), Cells.style(sheet, 2, 6),
                Cells.style(sheet, 3, 6), Cells.style(sheet, 3, 7), lastStyle(sheet, 6), lastStyle(sheet, 7)};
        Cells.clear(sheet, 0, CATEGORY_ROWS, 6, 7);

        String income = ModelSheets.INCOME_NAME;
        String expenses = ModelSheets.EXPENSES_NAME;
        Cells.number(sheet, 0, 3, ctx.data.period().year(), Cells.style(sheet, 0, 3));
        Cells.text(sheet, 1, 1, ctx.labels.get("budget", ctx.labels.month(ctx.data.period())), Cells.style(sheet, 1, 1));
        Cells.blank(sheet, 1, 2, Cells.style(sheet, 1, 2));
        Cells.text(sheet, 2, 1, ctx.labels.get("percentSpent"), Cells.style(sheet, 2, 1));
        Cells.formula(sheet, 4, 1, expenses + "+C10", Cells.style(sheet, 4, 1));
        Cells.formula(sheet, 4, 2, "IF(" + income + "=0,0,(" + expenses + "+C10)/" + income + ")", Cells.style(sheet, 4, 2));
        Cells.text(sheet, 6, 1, ctx.labels.get("summary"), Cells.style(sheet, 6, 1));
        Cells.text(sheet, 7, 1, ctx.labels.get("incomeTotal"), Cells.style(sheet, 7, 1));
        Cells.formula(sheet, 7, 2, income, Cells.style(sheet, 7, 2));
        Cells.text(sheet, 8, 1, ctx.labels.get("expensesTotal"), Cells.style(sheet, 8, 1));
        Cells.formula(sheet, 8, 2, expenses, Cells.style(sheet, 8, 2));
        Cells.text(sheet, 9, 1, ctx.labels.get("cardsTotal"), Cells.style(sheet, 9, 1));
        Cells.formula(sheet, 9, 2, "SUM(" + ctx.sheetRef("cards") + "!F3)", Cells.style(sheet, 9, 2));
        Cells.text(sheet, 11, 1, ctx.labels.get("balance"), Cells.style(sheet, 11, 1));
        Cells.formula(sheet, 11, 2, income + "-C9-C10", Cells.style(sheet, 11, 2));

        ctx.dialect.writeCategories(ctx, sheet, categoryStyles);
        renameChartReferences(sheet, ctx.labels.sheet("summary"));
    }

    private static CellStyle lastStyle(XSSFSheet sheet, int col) {
        CellStyle found = null;
        for (int r = 3; r <= CATEGORY_ROWS; r++) {
            CellStyle style = Cells.style(sheet, r, col);
            if (style != null) {
                found = style;
            }
        }
        return found;
    }

    /**
     * Points the chart series to the summary sheet under its current name (XLS-15).
     */
    private static void renameChartReferences(XSSFSheet sheet, String sheetName) {
        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing == null) {
            return;
        }
        String quoted = sheetName.matches("[A-Za-z0-9_]+") ? sheetName : Cells.sheetRef(sheetName);
        for (XSSFChart chart : drawing.getCharts()) {
            XmlObject[] refs = chart.getCTChartSpace().selectPath(
                    "declare namespace c='http://schemas.openxmlformats.org/drawingml/2006/chart' .//c:f");
            for (XmlObject ref : refs) {
                try (XmlCursor cursor = ref.newCursor()) {
                    String value = cursor.getTextValue();
                    int bang = value.lastIndexOf('!');
                    if (bang > 0) {
                        cursor.setTextValue(quoted + value.substring(bang));
                    }
                }
            }
        }
    }
}

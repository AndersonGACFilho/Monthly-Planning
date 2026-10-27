package com.planejamentomensal.statement.application.workbook;

import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * The formulas that differ between the two formula modes; the sheet writers call it and stay the same in both
 * (XLS-09, XLS-10, XLS-16).
 *
 * @since 1.0
 */
public interface FormulaDialect {

    /**
     * Returns the mode.
     *
     * @return formula mode
     */
    FormulaMode mode();

    /**
     * Writes the total of each card on the monthly expenses sheet, from a top-left cell.
     *
     * @param ctx       export context
     * @param sheet     monthly expenses sheet
     * @param row       first row (0-based)
     * @param col       card name column (0-based); the total goes in the next column
     * @param cardNames card names in the order of the cards sheet, the bills group excluded
     * @param styles    styles of the name and the total cells
     * @return last row written
     */
    int writeCardTotals(WorkbookContext ctx, XSSFSheet sheet, int row, int col, List<String> cardNames, CellStyle[] styles);

    /**
     * Writes the expense by category with the member filter on the summary sheet.
     *
     * @param ctx    export context
     * @param sheet  summary sheet
     * @param styles captured styles: filter label, filter value, header, label, value, total label, total value
     */
    void writeCategories(WorkbookContext ctx, XSSFSheet sheet, CellStyle[] styles);

    /**
     * Returns the formula of a defined total name (XLS-08).
     *
     * @param ctx   export context
     * @param table {@code Receita} or {@code Despesas}
     * @return formula without the equals sign
     */
    String totalName(WorkbookContext ctx, String table);
}

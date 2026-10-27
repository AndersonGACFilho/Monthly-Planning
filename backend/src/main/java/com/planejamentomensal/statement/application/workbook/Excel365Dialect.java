package com.planejamentomensal.statement.application.workbook;

import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataConsolidateFunction;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFPivotTable;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import javax.xml.namespace.QName;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPivotField;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.STFieldSortType;

/**
 * Excel 365 formulas, as in the original spreadsheet: card totals with {@code SORT}, {@code UNIQUE} and
 * {@code FILTER}, category expenses in a real pivot table filtered by member and sorted by value (XLS-09).
 *
 * @since 1.0
 */
public class Excel365Dialect implements FormulaDialect {

    /** Column of the member code in the cards table. */
    static final int MEMBER_COLUMN = 2;
    /** Column of the amount in the cards table. */
    static final int AMOUNT_COLUMN = 5;
    /** Column of the category in the cards table. */
    static final int CATEGORY_COLUMN = 6;
    /** Data field index used by the descending sort. */
    private static final long DATA_FIELD = 4294967294L;

    /**
     * Creates the dialect.
     */
    public Excel365Dialect() {
        // stateless
    }

    @Override
    public FormulaMode mode() {
        return FormulaMode.EXCEL_365;
    }

    @Override
    public int writeCardTotals(WorkbookContext ctx, XSSFSheet sheet, int row, int col, List<String> cardNames, CellStyle[] styles) {
        String table = ModelSheets.CARDS_TABLE;
        String card = table + "[" + ctx.labels.get("card") + "]";
        String amount = table + "[" + ctx.labels.get("amount") + "]";
        Cells.formula(sheet, row, col, "_xlfn._xlws.SORT(_xlfn.UNIQUE(_xlfn._xlws.FILTER(" + card + ",(" + card + "<>\"\")*("
                + card + "<>\"-\")*ISERROR(SEARCH(\"" + ctx.labels.get("bills") + "\"," + card + ")))))", styles[0]);
        Cells.dynamicArray(sheet, row, col);
        Cells.formula(sheet, row, col + 1, "SUMIF(" + card + ",_xlfn.ANCHORARRAY(" + Cells.col(col) + (row + 1) + ")," + amount + ")",
                styles[1]);
        Cells.dynamicArray(sheet, row, col + 1);
        int last = row + Math.max(cardNames.size(), 1) - 1;
        for (int r = row + 1; r <= last; r++) {
            Cells.blank(sheet, r, col, styles[0]);
            Cells.blank(sheet, r, col + 1, styles[1]);
        }
        return last;
    }

    @Override
    public void writeCategories(WorkbookContext ctx, XSSFSheet sheet, CellStyle[] styles) {
        XSSFTable table = Cells.table(ctx.workbook, ModelSheets.CARDS_TABLE);
        XSSFPivotTable pivot = sheet.createPivotTable(table, new CellReference("G3"));
        pivot.addReportFilter(MEMBER_COLUMN);
        pivot.addRowLabel(CATEGORY_COLUMN);
        pivot.addColumnLabel(DataConsolidateFunction.SUM, AMOUNT_COLUMN, ctx.labels.get("amount"));
        CTPivotField category = pivot.getCTPivotTableDefinition().getPivotFields().getPivotFieldArray(CATEGORY_COLUMN);
        category.setSortType(STFieldSortType.DESCENDING);
        sortByValue(category);
        pivot.getPivotCacheDefinition().getCTPivotCacheDefinition().setRefreshOnLoad(true);
        pivot.getCTPivotTableDefinition().setDataCaption(ctx.labels.get("amount"));
    }

    /**
     * Adds the auto-sort scope that orders the categories by the summed value, as in the model.
     */
    private static void sortByValue(CTPivotField field) {
        String ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        try (XmlCursor cursor = field.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(new QName(ns, "autoSortScope"));
            cursor.beginElement(new QName(ns, "pivotArea"));
            cursor.insertAttributeWithValue("dataOnly", "0");
            cursor.insertAttributeWithValue("outline", "0");
            cursor.insertAttributeWithValue("fieldPosition", "0");
            cursor.beginElement(new QName(ns, "references"));
            cursor.insertAttributeWithValue("count", "1");
            cursor.beginElement(new QName(ns, "reference"));
            cursor.insertAttributeWithValue("field", String.valueOf(DATA_FIELD));
            cursor.insertAttributeWithValue("count", "1");
            cursor.insertAttributeWithValue("selected", "0");
            cursor.beginElement(new QName(ns, "x"));
            cursor.insertAttributeWithValue("v", "0");
        }
    }

    @Override
    public String totalName(WorkbookContext ctx, String table) {
        return "SUM(" + table + "[" + ctx.labels.get("amount") + "])";
    }
}

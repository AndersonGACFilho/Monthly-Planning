package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.statement.application.workbook.WorkbookData.CardRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;

/**
 * Cards sheet: table {@code CARTÕES} grouped by card with a blank row between groups, the "Bills" group, the
 * general total and the total of each member by {@code SUMIF} (XLS-02 to XLS-05, XLS-07, XLS-08, XLS-11).
 *
 * @since 1.0
 */
public class CardsSheetWriter implements SheetWriter {

    /** Header row of the table (0-based). */
    static final int HEADER = 3;
    private static final int COLUMNS = 7;
    private static final int MEMBER_LABEL_COL = 8;
    private static final String[] HEADERS = {"card", "dueDay", "member", "installment", "description", "amount", "category"};

    /**
     * Creates the writer.
     */
    public CardsSheetWriter() {
        // stateless
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFSheet sheet = ctx.sheet("cards");
        XSSFTable table = Cells.table(ctx.workbook, ModelSheets.CARDS_TABLE);
        int oldLast = Math.max(table.getArea().getLastCell().getRow(), sheet.getLastRowNum());
        CellStyle[] data = new CellStyle[COLUMNS];
        for (int c = 0; c < COLUMNS; c++) {
            for (int r = HEADER + 1; r <= Math.min(oldLast, HEADER + 30) && data[c] == null; r++) {
                data[c] = Cells.style(sheet, r, c);
            }
        }
        CellStyle[] headers = new CellStyle[COLUMNS];
        for (int c = 0; c < COLUMNS; c++) {
            headers[c] = Cells.style(sheet, HEADER, c);
        }
        CellStyle totalLabel = Cells.style(sheet, 2, 4);
        CellStyle totalValue = Cells.style(sheet, 2, 5);
        CellStyle memberLabel = Cells.style(sheet, 2, MEMBER_LABEL_COL);
        CellStyle memberCode = Cells.style(sheet, 2, MEMBER_LABEL_COL + 1);
        CellStyle memberTotalLabel = Cells.style(sheet, 3, MEMBER_LABEL_COL);
        CellStyle memberTotal = Cells.style(sheet, 3, MEMBER_LABEL_COL + 1);
        Cells.clear(sheet, HEADER + 1, oldLast, 0, MEMBER_LABEL_COL + 30);
        Cells.clear(sheet, 2, 3, MEMBER_LABEL_COL, MEMBER_LABEL_COL + 20);

        Cells.text(sheet, 1, 0, ctx.labels.get("cardsTitle"), Cells.style(sheet, 1, 0));
        for (int c = 0; c < COLUMNS; c++) {
            Cells.text(sheet, HEADER, c, ctx.labels.get(HEADERS[c]), headers[c]);
        }
        Map<CellStyle, CellStyle> pending = new HashMap<>();
        int r = HEADER + 1;
        String group = null;
        List<CardRow> rows = ctx.data.cardRows();
        for (CardRow row : rows) {
            if (group != null && !Objects.equals(group, row.group())) {
                for (int c = 0; c < COLUMNS; c++) {
                    Cells.blank(sheet, r, c, data[c]);
                }
                r++;
            }
            group = row.group();
            CellStyle[] styles = data;
            if (row.pending()) {
                styles = new CellStyle[COLUMNS];
                for (int c = 0; c < COLUMNS; c++) {
                    styles[c] = Cells.pending(ctx.workbook, data[c], pending);
                }
            }
            Cells.text(sheet, r, 0, row.group(), styles[0]);
            Cells.number(sheet, r, 1, row.dueDay(), styles[1]);
            Cells.text(sheet, r, 2, row.memberCode(), styles[2]);
            Cells.text(sheet, r, 3, row.installment(), styles[3]);
            Cells.text(sheet, r, 4, row.description(), styles[4]);
            Cells.number(sheet, r, 5, row.amount().amount().doubleValue(), styles[5]);
            if (row.category() != null) {
                Cells.text(sheet, r, 6, row.category(), styles[6]);
            } else {
                Cells.blank(sheet, r, 6, styles[6]);
            }
            r++;
        }
        while (r <= HEADER + 2) {
            for (int c = 0; c < COLUMNS; c++) {
                Cells.blank(sheet, r, c, data[c]);
            }
            r++;
        }
        int last = r - 1;
        Cells.resize(table, HEADER, last);
        ctx.cardsLastRow = last;

        Cells.text(sheet, 2, 4, ctx.labels.get("total"), totalLabel);
        Cells.formula(sheet, 2, 5, "SUM(F" + (HEADER + 2) + ":F" + (last + 1) + ")", totalValue);
        Cells.text(sheet, 2, MEMBER_LABEL_COL, ctx.labels.get("memberUpper"), memberLabel);
        Cells.text(sheet, 3, MEMBER_LABEL_COL, ctx.labels.get("total"), memberTotalLabel);
        int col = MEMBER_LABEL_COL + 1;
        for (WorkbookData.Member member : ctx.data.members()) {
            String letter = Cells.col(col);
            Cells.text(sheet, 2, col, member.code(), memberCode);
            Cells.formula(sheet, 3, col, "SUMIF($C:$C," + letter + "3,$F:$F)", memberTotal);
            ctx.memberTotals.put(member.code(), ctx.sheetRef("cards") + "!$" + letter + "$4");
            col++;
        }

        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
        if (ctx.memberList != null) {
            var members = helper.createValidation(helper.createFormulaListConstraint(ctx.memberList),
                    new CellRangeAddressList(HEADER + 1, last, 2, 2));
            members.setShowErrorBox(false);
            sheet.addValidationData(members);
        }
        if (ctx.categoryList != null) {
            var categories = helper.createValidation(helper.createFormulaListConstraint(ctx.categoryList),
                    new CellRangeAddressList(HEADER + 1, last, 6, 6));
            categories.setShowErrorBox(false);
            sheet.addValidationData(categories);
        }
    }
}

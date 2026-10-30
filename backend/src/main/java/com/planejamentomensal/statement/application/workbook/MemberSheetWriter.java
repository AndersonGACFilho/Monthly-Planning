package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.statement.application.workbook.WorkbookData.CardRow;
import com.planejamentomensal.statement.application.workbook.WorkbookData.MemberSheet;
import com.planejamentomensal.statement.application.workbook.WorkbookData.MonthBlock;
import java.util.Map;
import java.util.HashMap;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * One sheet per member marked in the settings, copied from the member sheet of the model, with one block per
 * neighbouring month and its total; blocks alternate between a left and a right column (XLS-06, XLS-07).
 *
 * @since 1.0
 */
public class MemberSheetWriter implements SheetWriter {

    private static final int BLOCK_WIDTH = 5;
    private static final int GAP_COLUMNS = 1;
    private static final int GAP_ROWS = 2;
    private static final String[] HEADERS = {"card", "dueDay", "installment", "description", "amount"};

    /**
     * Creates the writer.
     */
    public MemberSheetWriter() {
        // stateless
    }

    /**
     * Returns the member sheet of the model: the first sheet that is not one of the fixed sheets.
     *
     * @param workbook model
     * @param ctx      export context
     * @return sheet index, or -1
     */
    static int templateIndex(XSSFWorkbook workbook, WorkbookContext ctx) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String name = workbook.getSheetName(i);
            boolean fixed = java.util.stream.Stream.of("summary", "income", "expenses", "cards", "lists")
                    .anyMatch(key -> ctx.labels.sheet(key).equals(name));
            if (!fixed) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFWorkbook workbook = ctx.workbook;
        int templateIndex = templateIndex(workbook, ctx);
        if (templateIndex < 0) {
            return;
        }
        XSSFSheet template = workbook.getSheetAt(templateIndex);
        CellStyle title = Cells.style(template, 1, 1);
        CellStyle total = Cells.style(template, 1, 5);
        CellStyle[] headers = new CellStyle[BLOCK_WIDTH];
        CellStyle[] row = new CellStyle[BLOCK_WIDTH];
        for (int c = 0; c < BLOCK_WIDTH; c++) {
            headers[c] = Cells.style(template, 2, 1 + c);
            row[c] = Cells.style(template, 3, 1 + c);
        }
        int position = templateIndex + 1;
        for (MemberSheet member : ctx.data.memberSheets()) {
            String name = uniqueName(workbook, member.name());
            XSSFSheet sheet = workbook.cloneSheet(templateIndex, name);
            workbook.setSheetOrder(name, position++);
            while (sheet.getNumMergedRegions() > 0) {
                sheet.removeMergedRegion(0);
            }
            Cells.clear(sheet, 0, Math.max(sheet.getLastRowNum(), 0), 0, 30);
            Map<CellStyle, CellStyle> pending = new HashMap<>();
            int[] nextRow = {1, 1};
            int index = 0;
            for (MonthBlock block : member.blocks()) {
                int side = index++ % 2;
                int col = 1 + side * (BLOCK_WIDTH + GAP_COLUMNS);
                int top = nextRow[side];
                Cells.text(sheet, top, col, ctx.labels.month(block.period()) + " " + block.period().year(), title);
                for (int c = 1; c < BLOCK_WIDTH - 1; c++) {
                    Cells.blank(sheet, top, col + c, title);
                }
                sheet.addMergedRegion(new CellRangeAddress(top, top, col, col + BLOCK_WIDTH - 2));
                for (int c = 0; c < BLOCK_WIDTH; c++) {
                    Cells.text(sheet, top + 1, col + c, ctx.labels.get(HEADERS[c]), headers[c]);
                }
                int r = top + 2;
                for (CardRow line : block.rows()) {
                    CellStyle[] styles = row;
                    if (line.pending()) {
                        styles = new CellStyle[BLOCK_WIDTH];
                        for (int c = 0; c < BLOCK_WIDTH; c++) {
                            styles[c] = Cells.pending(workbook, row[c], pending);
                        }
                    }
                    Cells.text(sheet, r, col, line.group(), styles[0]);
                    if (line.dueDay() > 0) {
                        Cells.number(sheet, r, col + 1, line.dueDay(), styles[1]);
                    } else {
                        Cells.blank(sheet, r, col + 1, styles[1]);
                    }
                    Cells.text(sheet, r, col + 2, line.installment(), styles[2]);
                    Cells.text(sheet, r, col + 3, line.description(), styles[3]);
                    Cells.number(sheet, r, col + 4, line.amount().amount().doubleValue(), styles[4]);
                    r++;
                }
                String amountCol = Cells.col(col + 4);
                Cells.formula(sheet, top, col + 4, block.rows().isEmpty() ? "0"
                        : "SUM(" + amountCol + (top + 3) + ":" + amountCol + r + ")", total);
                if (block.carriedOut() != null) {
                    Cells.text(sheet, r, col + 3, block.carriedOut().item(), row[3]);
                    Cells.number(sheet, r, col + 4, block.carriedOut().amount().amount().doubleValue(), row[4]);
                    r++;
                }
                nextRow[side] = r + GAP_ROWS;
            }
        }
        workbook.removeSheetAt(templateIndex);
    }

    /**
     * Keeps only the first block of the member sheet of a model, without data, so the model carries the look but no
     * one's figures (Q10).
     *
     * @param sheet member sheet of the model
     * @param ctx   context with the labels
     */
    static void trimTemplate(XSSFSheet sheet, WorkbookContext ctx) {
        CellStyle title = Cells.style(sheet, 1, 1);
        CellStyle total = Cells.style(sheet, 1, 5);
        CellStyle[] headers = new CellStyle[BLOCK_WIDTH];
        CellStyle[] row = new CellStyle[BLOCK_WIDTH];
        for (int c = 0; c < BLOCK_WIDTH; c++) {
            headers[c] = Cells.style(sheet, 2, 1 + c);
            row[c] = Cells.style(sheet, 3, 1 + c);
        }
        while (sheet.getNumMergedRegions() > 0) {
            sheet.removeMergedRegion(0);
        }
        Cells.clear(sheet, 0, Math.max(sheet.getLastRowNum(), 0), 0, 30);
        Cells.text(sheet, 1, 1, ctx.labels.month(ctx.data.period()), title);
        for (int c = 1; c < BLOCK_WIDTH - 1; c++) {
            Cells.blank(sheet, 1, 1 + c, title);
        }
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, BLOCK_WIDTH - 1));
        Cells.formula(sheet, 1, 5, "SUM(F4:F4)", total);
        for (int c = 0; c < BLOCK_WIDTH; c++) {
            Cells.text(sheet, 2, 1 + c, ctx.labels.get(HEADERS[c]), headers[c]);
            Cells.blank(sheet, 3, 1 + c, row[c]);
        }
    }

    private static String uniqueName(XSSFWorkbook workbook, String wanted) {
        String name = wanted.isBlank() ? "-" : wanted;
        int n = 2;
        while (workbook.getSheet(name) != null) {
            String suffix = " " + n++;
            name = (wanted.length() + suffix.length() > 31 ? wanted.substring(0, 31 - suffix.length()) : wanted) + suffix;
        }
        return name;
    }
}

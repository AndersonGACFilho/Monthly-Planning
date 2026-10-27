package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.statement.application.workbook.WorkbookData.CardRow;
import com.planejamentomensal.statement.application.workbook.WorkbookData.Line;
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;

/**
 * Monthly expenses sheet: table {@code Despesas} with the expenses of MAIN members paid outside the cards, and the
 * total of each card written by the formula dialect (XLS-03, XLS-08, XLS-09).
 *
 * @since 1.0
 */
public class ExpensesSheetWriter implements SheetWriter {

    private static final int HEADER = 2;
    private static final int CARD_COL = 4;

    /**
     * Creates the writer.
     */
    public ExpensesSheetWriter() {
        // stateless
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFSheet sheet = ctx.sheet("expenses");
        XSSFTable table = Cells.table(ctx.workbook, ModelSheets.EXPENSES_TABLE);
        int tableLast = table.getArea().getLastCell().getRow();
        int oldLast = Math.max(tableLast, sheet.getLastRowNum());
        CellStyle[] first = {Cells.style(sheet, HEADER + 1, 1), Cells.style(sheet, HEADER + 1, 2)};
        CellStyle[] middle = {Cells.style(sheet, HEADER + 2, 1), Cells.style(sheet, HEADER + 2, 2)};
        CellStyle[] lastStyle = {Cells.style(sheet, tableLast, 1), Cells.style(sheet, tableLast, 2)};
        CellStyle[] card = {Cells.style(sheet, HEADER + 2, CARD_COL), Cells.style(sheet, HEADER + 2, CARD_COL + 1)};
        CellStyle[] firstCard = {Cells.style(sheet, HEADER + 1, CARD_COL), Cells.style(sheet, HEADER + 1, CARD_COL + 1)};
        CellStyle[] headers = {Cells.style(sheet, HEADER, 1), Cells.style(sheet, HEADER, 2), Cells.style(sheet, HEADER, CARD_COL),
                Cells.style(sheet, HEADER, CARD_COL + 1)};
        Cells.clear(sheet, HEADER + 1, oldLast, 0, CARD_COL + 8);

        Cells.text(sheet, 1, 1, ctx.labels.get("expensesTitle"), Cells.style(sheet, 1, 1));
        Cells.text(sheet, 1, CARD_COL, ctx.labels.get("cardsTitle"), Cells.style(sheet, 1, CARD_COL));
        Cells.text(sheet, HEADER, 1, ctx.labels.get("item"), headers[0]);
        Cells.text(sheet, HEADER, 2, ctx.labels.get("amount"), headers[1]);
        Cells.text(sheet, HEADER, CARD_COL, ctx.labels.get("cardUpper"), headers[2]);
        Cells.text(sheet, HEADER, CARD_COL + 1, ctx.labels.get("total"), headers[3]);

        List<Line> lines = ctx.data.expenses();
        int count = Math.max(lines.size(), 2);
        for (int i = 0; i < count; i++) {
            CellStyle[] s = i == 0 ? first : i == count - 1 ? lastStyle : middle;
            int r = HEADER + 1 + i;
            if (i < lines.size()) {
                Cells.text(sheet, r, 1, lines.get(i).item(), s[0]);
                Cells.number(sheet, r, 2, lines.get(i).amount().amount().doubleValue(), s[1]);
            } else {
                Cells.blank(sheet, r, 1, s[0]);
                Cells.blank(sheet, r, 2, s[1]);
            }
        }
        Cells.resize(table, HEADER, HEADER + count);
        ctx.expensesLastRow = HEADER + count;

        String bills = ctx.labels.get("bills");
        List<String> cards = new LinkedHashSet<>(ctx.data.cardRows().stream().map(CardRow::group)
                .filter(g -> !g.equals(bills)).toList()).stream().toList();
        CellStyle[] styles = {firstCard[0] != null ? firstCard[0] : card[0], firstCard[1] != null ? firstCard[1] : card[1]};
        ctx.dialect.writeCardTotals(ctx, sheet, HEADER + 1, CARD_COL, cards, styles);
    }
}

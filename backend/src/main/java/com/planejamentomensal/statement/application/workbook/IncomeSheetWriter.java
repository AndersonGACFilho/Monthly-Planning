package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.statement.application.workbook.WorkbookData.Line;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;

/**
 * Monthly income sheet: table {@code Receita} with the incomes of MAIN members and one reimbursement line per third
 * party, which points to that member's total on the cards sheet, then what third parties carried in from the previous
 * month and out to the next one (RF-28), beside the table of what each third party owes (XLS-04, XLS-08).
 *
 * @since 1.0
 */
public class IncomeSheetWriter implements SheetWriter {

    private static final int HEADER = 2;

    /**
     * Creates the writer.
     */
    public IncomeSheetWriter() {
        // stateless
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFSheet sheet = ctx.sheet("income");
        XSSFTable table = Cells.table(ctx.workbook, ModelSheets.INCOME_TABLE);
        int oldLast = Math.max(table.getArea().getLastCell().getRow(), sheet.getLastRowNum());
        CellStyle[][] rowStyles = {{Cells.style(sheet, 3, 1), Cells.style(sheet, 3, 2), Cells.style(sheet, 3, 5),
                Cells.style(sheet, 3, 6), Cells.style(sheet, 3, 7)}, {Cells.style(sheet, 4, 1), Cells.style(sheet, 4, 2),
                        Cells.style(sheet, 4, 5), Cells.style(sheet, 4, 6), Cells.style(sheet, 4, 7)}};
        for (CellStyle[] styles : rowStyles) {
            styles[2] = styles[2] != null ? styles[2] : styles[0];
            styles[3] = styles[3] != null ? styles[3] : styles[0];
            styles[4] = styles[4] != null ? styles[4] : styles[1];
        }
        CellStyle header = Cells.style(sheet, HEADER, 1);
        CellStyle sideHeader = Cells.style(sheet, HEADER, 5);
        CellStyle totalLabel = Cells.style(sheet, 1, 6);
        CellStyle totalValue = Cells.style(sheet, 1, 7);
        Cells.clear(sheet, HEADER + 1, oldLast, 0, 12);
        Cells.clear(sheet, 1, 1, 6, 7);

        Cells.text(sheet, 1, 1, ctx.labels.get("incomeTitle"), Cells.style(sheet, 1, 1));
        Cells.text(sheet, HEADER, 1, ctx.labels.get("item"), header);
        Cells.text(sheet, HEADER, 2, ctx.labels.get("amount"), Cells.style(sheet, HEADER, 2));
        Cells.text(sheet, HEADER, 5, ctx.labels.get("member"), sideHeader);
        Cells.text(sheet, HEADER, 6, ctx.labels.get("person"), sideHeader);
        Cells.text(sheet, HEADER, 7, ctx.labels.get("amount"), sideHeader);

        List<WorkbookData.Member> thirdParties = ctx.data.members().stream().filter(m -> !m.main()).toList();
        int side = HEADER + 1;
        for (WorkbookData.Member member : thirdParties) {
            CellStyle[] s = rowStyles[(side - HEADER - 1) % 2];
            Cells.text(sheet, side, 5, member.code(), s[2]);
            Cells.text(sheet, side, 6, member.name(), s[3]);
            Cells.formula(sheet, side, 7, ctx.memberTotals.getOrDefault(member.code(), "0"), s[4]);
            side++;
        }
        Cells.text(sheet, 1, 6, ctx.labels.get("total"), totalLabel);
        Cells.formula(sheet, 1, 7, "SUM(H" + (HEADER + 2) + ":H" + Math.max(side, HEADER + 2) + ")", totalValue);

        int r = HEADER + 1;
        for (Line line : ctx.data.incomes()) {
            CellStyle[] s = rowStyles[(r - HEADER - 1) % 2];
            Cells.text(sheet, r, 1, line.item(), s[0]);
            Cells.number(sheet, r, 2, line.amount().amount().doubleValue(), s[1]);
            r++;
        }
        int sideRow = HEADER + 1;
        for (WorkbookData.Member member : thirdParties) {
            CellStyle[] s = rowStyles[(r - HEADER - 1) % 2];
            Cells.text(sheet, r, 1, ctx.labels.get("reimbursement", member.code() + " — " + member.name()), s[0]);
            Cells.formula(sheet, r, 2, "H" + (sideRow + 1), s[1]);
            r++;
            sideRow++;
        }
        for (Line line : ctx.data.carries()) {
            CellStyle[] s = rowStyles[(r - HEADER - 1) % 2];
            Cells.text(sheet, r, 1, line.item(), s[0]);
            Cells.number(sheet, r, 2, line.amount().amount().doubleValue(), s[1]);
            r++;
        }
        while (r <= HEADER + 2) {
            CellStyle[] s = rowStyles[(r - HEADER - 1) % 2];
            Cells.blank(sheet, r, 1, s[0]);
            Cells.blank(sheet, r, 2, s[1]);
            r++;
        }
        Cells.resize(table, HEADER, r - 1);
        ctx.incomeLastRow = r - 1;
    }
}

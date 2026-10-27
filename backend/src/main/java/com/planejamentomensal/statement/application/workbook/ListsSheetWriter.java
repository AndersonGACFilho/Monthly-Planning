package com.planejamentomensal.statement.application.workbook;

import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;

/**
 * Lists sheet: table {@code Categorias} and the member codes, sources of the drop-down lists (XLS-11).
 *
 * @since 1.0
 */
public class ListsSheetWriter implements SheetWriter {

    /**
     * Creates the writer.
     */
    public ListsSheetWriter() {
        // stateless
    }

    @Override
    public void write(WorkbookContext ctx) {
        XSSFSheet sheet = ctx.sheet("lists");
        XSSFTable table = Cells.table(ctx.workbook, ModelSheets.CATEGORIES_TABLE);
        CellStyle header = Cells.style(sheet, 0, 0);
        CellStyle item = Cells.style(sheet, 1, 0);
        Cells.clear(sheet, 0, Math.max(sheet.getLastRowNum(), 1), 0, 2);
        Cells.text(sheet, 0, 0, ctx.labels.get("category"), header);
        List<String> categories = ctx.data.categories();
        for (int i = 0; i < categories.size(); i++) {
            Cells.text(sheet, i + 1, 0, categories.get(i), item);
        }
        int lastCategory = Math.max(categories.size(), 1);
        if (categories.isEmpty()) {
            Cells.blank(sheet, 1, 0, item);
        }
        Cells.resize(table, 0, lastCategory);
        ctx.categoryList = ctx.sheetRef("lists") + "!$A$2:$A$" + (lastCategory + 1);

        Cells.text(sheet, 0, 2, ctx.labels.get("member"), header);
        List<WorkbookData.Member> members = ctx.data.members();
        for (int i = 0; i < members.size(); i++) {
            Cells.text(sheet, i + 1, 2, members.get(i).code(), item);
        }
        ctx.memberList = ctx.sheetRef("lists") + "!$C$2:$C$" + (Math.max(members.size(), 1) + 1);
    }
}

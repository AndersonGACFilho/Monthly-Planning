package com.planejamentomensal.statement.application.workbook;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * State shared by the sheet writers of one export: the workbook, the data, the labels, the formula dialect and the
 * cells one sheet publishes for the next ones.
 *
 * @since 1.0
 */
public final class WorkbookContext {

    final XSSFWorkbook workbook;
    final WorkbookData data;
    final WorkbookLabels labels;
    final FormulaDialect dialect;
    /** Cell with each member's total on the cards sheet, such as {@code 'Cartões'!$J$4}. */
    final Map<String, String> memberTotals = new LinkedHashMap<>();
    /** Address of the member code list on the lists sheet. */
    String memberList;
    /** Address of the category list on the lists sheet. */
    String categoryList;
    /** Last data row (0-based) of the cards table. */
    int cardsLastRow;
    /** Last data row (0-based) of the income table. */
    int incomeLastRow;
    /** Last data row (0-based) of the expenses table. */
    int expensesLastRow;

    WorkbookContext(XSSFWorkbook workbook, WorkbookData data, WorkbookLabels labels, FormulaDialect dialect) {
        this.workbook = workbook;
        this.data = data;
        this.labels = labels;
        this.dialect = dialect;
    }

    XSSFSheet sheet(String key) {
        return workbook.getSheet(labels.sheet(key));
    }

    String sheetRef(String key) {
        return Cells.sheetRef(labels.sheet(key));
    }

    String mainMemberCode() {
        return data.members().stream().filter(WorkbookData.Member::main).map(WorkbookData.Member::code).findFirst().orElse("");
    }
}

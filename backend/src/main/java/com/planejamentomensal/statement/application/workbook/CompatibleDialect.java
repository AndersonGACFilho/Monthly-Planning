package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;

/**
 * Formulas that LibreOffice and older Excel read: the card list already sorted with one {@code SUMIF} per row, and a
 * {@code SUMIFS} table by category with the member in a drop-down list, sorted when exported (XLS-09).
 *
 * @since 1.0
 */
public class CompatibleDialect implements FormulaDialect {

    /**
     * Creates the dialect.
     */
    public CompatibleDialect() {
        // stateless
    }

    @Override
    public FormulaMode mode() {
        return FormulaMode.COMPATIBLE;
    }

    @Override
    public int writeCardTotals(WorkbookContext ctx, XSSFSheet sheet, int row, int col, List<String> cardNames, CellStyle[] styles) {
        String cards = ctx.sheetRef("cards");
        List<String> sorted = cardNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        int r = row;
        for (String name : sorted) {
            Cells.text(sheet, r, col, name, styles[0]);
            Cells.formula(sheet, r, col + 1, "SUMIF(" + cards + "!$A:$A," + Cells.col(col) + (r + 1) + "," + cards + "!$F:$F)", styles[1]);
            r++;
        }
        if (sorted.isEmpty()) {
            Cells.blank(sheet, row, col, styles[0]);
            Cells.blank(sheet, row, col + 1, styles[1]);
            return row;
        }
        return r - 1;
    }

    @Override
    public void writeCategories(WorkbookContext ctx, XSSFSheet sheet, CellStyle[] styles) {
        String cards = ctx.sheetRef("cards");
        String main = ctx.mainMemberCode();
        Cells.text(sheet, 0, 6, ctx.labels.get("member"), styles[0]);
        Cells.text(sheet, 0, 7, main, styles[1]);
        if (ctx.memberList != null) {
            XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
            var validation = helper.createValidation(helper.createFormulaListConstraint(ctx.memberList), new CellRangeAddressList(0, 0, 7, 7));
            validation.setShowErrorBox(true);
            sheet.addValidationData(validation);
        }
        Cells.text(sheet, 2, 6, ctx.labels.get("category"), styles[2]);
        Cells.text(sheet, 2, 7, ctx.labels.get("amount"), styles[2]);
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        ctx.data.cardRows().stream().filter(r -> r.category() != null)
                .forEach(r -> totals.merge(r.category(), r.memberCode().equals(main) ? r.amount().amount() : BigDecimal.ZERO,
                        BigDecimal::add));
        List<String> categories = totals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey).toList();
        int r = 3;
        for (String category : categories) {
            Cells.text(sheet, r, 6, category, styles[3]);
            Cells.formula(sheet, r, 7, "SUMIFS(" + cards + "!$F:$F," + cards + "!$G:$G,G" + (r + 1) + "," + cards + "!$C:$C,$H$1)",
                    styles[4]);
            r++;
        }
        if (categories.isEmpty()) {
            Cells.blank(sheet, r, 6, styles[3]);
            Cells.blank(sheet, r, 7, styles[4]);
            r++;
        }
        Cells.text(sheet, r, 6, ctx.labels.get("total"), styles[5]);
        Cells.formula(sheet, r, 7, "SUM(H4:H" + r + ")", styles[6]);
    }

    @Override
    public String totalName(WorkbookContext ctx, String table) {
        boolean income = ModelSheets.INCOME_TABLE.equals(table);
        String sheet = ctx.sheetRef(income ? "income" : "expenses");
        int last = (income ? ctx.incomeLastRow : ctx.expensesLastRow) + 1;
        return "SUM(" + sheet + "!$C$4:$C$" + Math.max(last, 4) + ")";
    }

    /**
     * Sums money values, for tests.
     *
     * @param values values
     * @return total
     */
    static Money sum(List<Money> values) {
        return values.stream().filter(Objects::nonNull).reduce(Money.ZERO, Money::plus);
    }

    /**
     * Orders categories by value, for tests.
     *
     * @param totals totals by category
     * @return category names, largest first
     */
    static List<String> order(Map<String, BigDecimal> totals) {
        return totals.entrySet().stream().sorted(Comparator.comparing(Map.Entry<String, BigDecimal>::getValue).reversed())
                .map(Map.Entry::getKey).toList();
    }
}

package com.planejamentomensal.statement.application.workbook;

import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula;

/**
 * Small cell helpers shared by the sheet writers. Formulas are stored as written, without POI parsing, so
 * Excel 365 functions and structured references stay intact.
 *
 * @since 1.0
 */
final class Cells {

    private Cells() {
    }

    static XSSFCell cell(XSSFSheet sheet, int row, int col) {
        XSSFRow r = sheet.getRow(row);
        if (r == null) {
            r = sheet.createRow(row);
        }
        if (r.getZeroHeight()) {
            r.setZeroHeight(false);
        }
        XSSFCell c = r.getCell(col);
        return c == null ? r.createCell(col) : c;
    }

    static CellStyle style(XSSFSheet sheet, int row, int col) {
        XSSFRow r = sheet.getRow(row);
        XSSFCell c = r == null ? null : r.getCell(col);
        return c == null ? null : c.getCellStyle();
    }

    static void text(XSSFSheet sheet, int row, int col, String value, CellStyle style) {
        XSSFCell c = cell(sheet, row, col);
        c.setCellValue(value == null ? "" : value);
        apply(c, style);
    }

    static void number(XSSFSheet sheet, int row, int col, double value, CellStyle style) {
        XSSFCell c = cell(sheet, row, col);
        c.setCellValue(value);
        apply(c, style);
    }

    static void formula(XSSFSheet sheet, int row, int col, String formula, CellStyle style) {
        XSSFCell c = cell(sheet, row, col);
        c.setBlank();
        CTCellFormula f = c.getCTCell().isSetF() ? c.getCTCell().getF() : c.getCTCell().addNewF();
        f.setStringValue(formula);
        if (c.getCTCell().isSetV()) {
            c.getCTCell().unsetV();
        }
        if (c.getCTCell().isSetT()) {
            c.getCTCell().unsetT();
        }
        apply(c, style);
    }

    /**
     * Marks a formula cell as a dynamic array that spills (Excel 365).
     */
    static void dynamicArray(XSSFSheet sheet, int row, int col) {
        cell(sheet, row, col).getCTCell().setCm(1);
    }

    static void blank(XSSFSheet sheet, int row, int col, CellStyle style) {
        XSSFCell c = cell(sheet, row, col);
        c.setBlank();
        if (c.getCTCell().isSetF()) {
            c.getCTCell().unsetF();
        }
        apply(c, style);
    }

    /**
     * Removes the cells of a rectangle, values, formulas and styles.
     */
    static void clear(XSSFSheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        for (int r = firstRow; r <= lastRow; r++) {
            XSSFRow row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            for (int c = firstCol; c <= lastCol; c++) {
                XSSFCell cell = row.getCell(c);
                if (cell != null) {
                    row.removeCell(cell);
                }
            }
            if (row.getPhysicalNumberOfCells() == 0) {
                sheet.removeRow(row);
            }
        }
    }

    static XSSFTable table(XSSFWorkbook workbook, String name) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            for (XSSFTable table : workbook.getSheetAt(i).getTables()) {
                if (name.equalsIgnoreCase(table.getName())) {
                    return table;
                }
            }
        }
        return null;
    }

    /**
     * Moves a table over new rows, keeping its columns, and takes the column names from the header cells.
     */
    static void resize(XSSFTable table, int headerRow, int lastRow) {
        AreaReference area = table.getArea();
        int firstCol = area.getFirstCell().getCol();
        int lastCol = area.getLastCell().getCol();
        AreaReference next = new AreaReference(new CellReference(headerRow, firstCol), new CellReference(Math.max(lastRow, headerRow + 1), lastCol),
                SpreadsheetVersion.EXCEL2007);
        table.setArea(next);
        if (table.getCTTable().isSetAutoFilter()) {
            table.getCTTable().getAutoFilter().setRef(next.formatAsString().replace("$", ""));
        }
        if (table.getCTTable().isSetTotalsRowCount()) {
            table.getCTTable().unsetTotalsRowCount();
        }
        table.getCTTable().setTotalsRowShown(false);
        table.updateHeaders();
    }

    /**
     * Returns a copy of a style with a yellow fill, for entries pending review (XLS-07).
     */
    static CellStyle pending(XSSFWorkbook workbook, CellStyle base, java.util.Map<CellStyle, CellStyle> cache) {
        return cache.computeIfAbsent(base, b -> {
            XSSFCellStyle style = workbook.createCellStyle();
            if (b != null) {
                style.cloneStyleFrom(b);
            }
            style.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[] {(byte) 0xFF, (byte) 0xF2, (byte) 0x9E}, null));
            style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            return style;
        });
    }

    /**
     * Quotes a sheet name for formulas.
     */
    static String sheetRef(String sheetName) {
        return "'" + sheetName.replace("'", "''") + "'";
    }

    /**
     * Returns a column letter, such as {@code F} for 5.
     */
    static String col(int index) {
        return CellReference.convertNumToColString(index);
    }

    private static void apply(XSSFCell cell, CellStyle style) {
        if (style != null) {
            cell.setCellStyle(style);
        }
    }
}

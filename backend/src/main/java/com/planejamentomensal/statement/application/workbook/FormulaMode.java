package com.planejamentomensal.statement.application.workbook;

import java.util.Locale;

/**
 * Formula mode of the monthly spreadsheet (XLS-09).
 *
 * @since 1.0
 */
public enum FormulaMode {
    /** Dynamic arrays ({@code SORT}, {@code UNIQUE}, {@code FILTER}) and a real pivot table. */
    EXCEL_365,
    /** Plain {@code SUMIF}/{@code SUMIFS}, readable by LibreOffice and older Excel. */
    COMPATIBLE;

    /**
     * Default mode of an operating system: Windows → Excel 365, others → Compatible (XLS-09).
     *
     * @param osName value of {@code os.name}
     * @return mode
     */
    public static FormulaMode defaultFor(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).startsWith("windows") ? EXCEL_365 : COMPATIBLE;
    }
}

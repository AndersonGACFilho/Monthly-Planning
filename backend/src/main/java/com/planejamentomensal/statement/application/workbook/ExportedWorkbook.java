package com.planejamentomensal.statement.application.workbook;

/**
 * A generated monthly spreadsheet (XLS-01).
 *
 * @param fileName     file name, such as {@code 2026 - 11 - NOVEMBRO CJ.xlsx}
 * @param content      XLSX bytes
 * @param mode         formula mode used (XLS-10)
 * @param pendingCount entries pending review highlighted in yellow (XLS-07)
 * @since 1.0
 */
public record ExportedWorkbook(String fileName, byte[] content, FormulaMode mode, int pendingCount) {
}

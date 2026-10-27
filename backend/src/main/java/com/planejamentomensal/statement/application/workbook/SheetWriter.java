package com.planejamentomensal.statement.application.workbook;

/**
 * Fills one sheet of the monthly spreadsheet; a new sheet is a new writer, the others stay untouched (XLS-16).
 *
 * @since 1.0
 */
public interface SheetWriter {

    /**
     * Writes the sheet.
     *
     * @param ctx export context
     */
    void write(WorkbookContext ctx);
}

package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.application.workbook.FormulaMode;
import org.springframework.hateoas.RepresentationModel;

/**
 * What the spreadsheet export dialog shows before downloading (XLS-07, XLS-10).
 *
 * @since 1.0
 */
public class WorkbookOptionsModel extends RepresentationModel<WorkbookOptionsModel> {

    /** Formula mode used when none is chosen. */
    public final FormulaMode defaultMode;
    /** Entries pending review that will be highlighted (XLS-07). */
    public final int pendingCount;

    /**
     * Creates the options.
     *
     * @param defaultMode  default mode
     * @param pendingCount pending entries
     */
    public WorkbookOptionsModel(FormulaMode defaultMode, int pendingCount) {
        this.defaultMode = defaultMode;
        this.pendingCount = pendingCount;
    }
}

package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import java.util.List;

/**
 * Runs after an import is confirmed, such as storing the aliases chosen in the preview (RF-70, SOL-02).
 *
 * @since 1.0
 */
public interface ConfirmationListener {

    /**
     * Called inside the confirmation transaction.
     *
     * @param batch confirmed batch
     * @param lines lines of the batch, with the entries created or confirmed
     */
    void confirmed(ImportBatch batch, List<ImportLine> lines);
}

package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.nio.file.Path;

/**
 * Files of the statements inside the portable {@code data/} folder (POR-03, POR-04, POR-05).
 *
 * @since 1.0
 */
public interface StatementFiles {

    /**
     * Returns the folder with the template fonts, copying the bundled fonts on first use (POR-05).
     *
     * @return fonts folder
     */
    Path fonts();

    /**
     * Saves an exported file under {@code data/exports/<billing-period>/} (RF-89, POR-04).
     *
     * @param period   billing period
     * @param baseName name without extension; it is sanitized
     * @param extension extension without the dot
     * @param content  file content
     * @return the saved file
     */
    Path saveExport(BillingPeriod period, String baseName, String extension, byte[] content);
}

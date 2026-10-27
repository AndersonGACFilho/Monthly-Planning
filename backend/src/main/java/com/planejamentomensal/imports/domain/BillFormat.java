package com.planejamentomensal.imports.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * File formats of Itaú bills (RF-61).
 *
 * @since 1.0
 */
public enum BillFormat {
    /** Closed bill as PDF. */
    PDF,
    /** Open bill exported as XLSX from the app. */
    XLSX;

    /**
     * Detects the format by file extension (RF-61).
     *
     * @param fileName uploaded file name
     * @return the format, or empty for other extensions
     */
    public static Optional<BillFormat> fromFileName(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT).strip();
        if (name.endsWith(".pdf")) {
            return Optional.of(PDF);
        }
        if (name.endsWith(".xlsx")) {
            return Optional.of(XLSX);
        }
        return Optional.empty();
    }
}

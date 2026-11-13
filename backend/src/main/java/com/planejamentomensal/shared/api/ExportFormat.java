package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Formats of the data export (RNF-07, IMP-69): every table as JSON or as CSV files in a ZIP, or the whole database as
 * one SQLite file.
 *
 * @since 1.0
 */
public enum ExportFormat {

    /** Every table in one JSON document. */
    JSON("application/json", "planejamento-mensal-dados.json"),
    /** One CSV file per table, zipped. */
    CSV("application/zip", "planejamento-mensal-dados.zip"),
    /** The whole database as a SQLite file, dated so exports of different days never overwrite each other. */
    DB("application/vnd.sqlite3", "planejamento-mensal-%s.db");

    private final String mediaType;
    private final String fileName;

    ExportFormat(String mediaType, String fileName) {
        this.mediaType = mediaType;
        this.fileName = fileName;
    }

    /**
     * Finds a format by name, ignoring case.
     *
     * @param name {@code json}, {@code csv} or {@code db}
     * @return the format
     * @throws InvalidInputException with code {@code export-format-invalid} for any other name
     */
    public static ExportFormat of(String name) {
        return Arrays.stream(values()).filter(f -> f.name().equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new InvalidInputException("export-format-invalid", name));
    }

    /**
     * Returns the media type of the file.
     *
     * @return media type
     */
    public String mediaType() {
        return mediaType;
    }

    /**
     * Returns the file name suggested to the user (POR-04).
     *
     * @param day day of the export
     * @return file name
     */
    public String fileName(LocalDate day) {
        return fileName.formatted(day);
    }
}

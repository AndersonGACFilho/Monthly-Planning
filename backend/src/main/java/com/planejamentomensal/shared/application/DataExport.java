package com.planejamentomensal.shared.application;

/**
 * Export of every table, so the data never depends on the app (RNF-07).
 *
 * @since 1.0
 */
public interface DataExport {

    /**
     * Exports all tables as one JSON document: {@code {"table": [{"column": value}]}}.
     *
     * @return UTF-8 JSON
     */
    byte[] json();

    /**
     * Exports all tables as a ZIP with one CSV file per table.
     *
     * @return ZIP bytes
     */
    byte[] csvZip();
}

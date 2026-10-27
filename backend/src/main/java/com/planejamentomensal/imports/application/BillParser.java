package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ParsedBill;

/**
 * Reads one bill format into lines, without business rules (RNF-08, SOL-02). Implementations live in
 * {@code infra}; a new bank or format is a new {@code @Component}.
 *
 * @since 1.0
 */
public interface BillParser {

    /**
     * Returns the format this parser reads.
     *
     * @return format
     */
    BillFormat format();

    /**
     * Reads a bill file kept only in memory (RNF-04).
     *
     * @param content file bytes
     * @return what the file contains
     * @throws com.planejamentomensal.shared.domain.InvalidInputException with code {@code invalid-bill-file}
     *                                                                    if the structure is not recognized
     *                                                                    (RF-61)
     */
    ParsedBill parse(byte[] content);
}

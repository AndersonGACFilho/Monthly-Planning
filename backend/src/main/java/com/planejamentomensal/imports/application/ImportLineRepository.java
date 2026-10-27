package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.ImportLine;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of preview lines (SOL-04).
 *
 * @since 1.0
 */
public interface ImportLineRepository {

    /**
     * Finds a line.
     *
     * @param id id
     * @return the line, or empty
     */
    Optional<ImportLine> findById(Long id);

    /**
     * Lists the lines of a batch in file order.
     *
     * @param batchId batch
     * @return lines
     */
    List<ImportLine> findByBatchIdOrderBySequence(long batchId);

    /**
     * Stores a line.
     *
     * @param line line
     * @return the stored line
     */
    ImportLine save(ImportLine line);

    /**
     * Stores lines.
     *
     * @param lines lines
     * @param <S>   line type
     * @return the stored lines
     */
    <S extends ImportLine> List<S> saveAll(Iterable<S> lines);
}

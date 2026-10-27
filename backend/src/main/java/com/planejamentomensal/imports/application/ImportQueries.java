package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to import batches and their lines.
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class ImportQueries {

    private final ImportBatchRepository batches;
    private final ImportLineRepository lines;

    /**
     * Creates the queries.
     *
     * @param batches batch storage
     * @param lines   line storage
     */
    public ImportQueries(ImportBatchRepository batches, ImportLineRepository lines) {
        this.batches = batches;
        this.lines = lines;
    }

    /**
     * Loads a batch.
     *
     * @param id id
     * @return the batch
     * @throws NotFoundException if it does not exist
     */
    public ImportBatch get(long id) {
        return batches.findById(id).orElseThrow(() -> new NotFoundException("import", id));
    }

    /**
     * Lists the lines of a batch.
     *
     * @param batchId batch
     * @return lines in file order
     */
    public List<ImportLine> lines(long batchId) {
        return lines.findByBatchIdOrderBySequence(batchId);
    }

    /**
     * Tells whether a confirmed batch is the latest of its card, the only one that can be undone (Q7).
     *
     * @param batch batch
     * @return {@code true} if it can be undone
     */
    public boolean isUndoable(ImportBatch batch) {
        if (batch.status() != BatchStatus.CONFIRMED) {
            return false;
        }
        List<ImportBatch> confirmed = batches.findByCardIdAndStatusOrderByIdDesc(batch.cardId(), BatchStatus.CONFIRMED);
        return !confirmed.isEmpty() && confirmed.getFirst().id().equals(batch.id());
    }
}

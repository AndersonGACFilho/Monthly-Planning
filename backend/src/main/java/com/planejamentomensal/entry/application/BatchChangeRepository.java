package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.BatchChange;
import java.util.List;

/**
 * Storage port of the changes made by import batches (RF-72).
 *
 * @since 1.0
 */
public interface BatchChangeRepository {

    /**
     * Lists the changes of a batch, latest first.
     *
     * @param batchId batch
     * @return changes in undo order
     */
    List<BatchChange> findByBatchIdOrderByIdDesc(long batchId);

    /**
     * Stores a change.
     *
     * @param change change
     * @return the stored change
     */
    BatchChange save(BatchChange change);

    /**
     * Removes every change of a batch.
     *
     * @param batchId batch
     */
    void deleteByBatchId(long batchId);
}

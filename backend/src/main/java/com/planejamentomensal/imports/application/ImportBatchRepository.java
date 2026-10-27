package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Storage port of import batches (SOL-04).
 *
 * @since 1.0
 */
public interface ImportBatchRepository {

    /**
     * Finds a batch.
     *
     * @param id id
     * @return the batch, or empty
     */
    Optional<ImportBatch> findById(Long id);

    /**
     * Lists batches.
     *
     * @param pageable page and sort
     * @return one page
     */
    Page<ImportBatch> findAll(Pageable pageable);

    /**
     * Lists the batches of a card in a state, latest first.
     *
     * @param cardId card
     * @param status state
     * @return batches
     */
    List<ImportBatch> findByCardIdAndStatusOrderByIdDesc(long cardId, BatchStatus status);

    /**
     * Finds batches of a file in a state (RF-73).
     *
     * @param fileHash file hash
     * @param status   state
     * @return batches
     */
    List<ImportBatch> findByFileHashAndStatus(String fileHash, BatchStatus status);

    /**
     * Stores a batch.
     *
     * @param batch batch
     * @return the stored batch
     */
    ImportBatch save(ImportBatch batch);
}

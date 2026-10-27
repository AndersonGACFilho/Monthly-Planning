package com.planejamentomensal.imports.application;

import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.domain.LineDecision;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edits a preview line, cancels a preview and undoes a confirmed batch (RF-69, RF-70, RF-72, RF-79c, Q7).
 *
 * @since 1.0
 */
@Service
public class ManageImportUseCase {

    private final ImportQueries queries;
    private final ImportBatchRepository batches;
    private final ImportLineRepository lines;
    private final EntryLedger ledger;

    /**
     * Creates the use case.
     *
     * @param queries batch queries
     * @param batches batch storage
     * @param lines   line storage
     * @param ledger  writer of entries
     */
    public ManageImportUseCase(ImportQueries queries, ImportBatchRepository batches, ImportLineRepository lines,
            EntryLedger ledger) {
        this.queries = queries;
        this.batches = batches;
        this.lines = lines;
        this.ledger = ledger;
    }

    /**
     * Changes the decision and suggestions of a preview line.
     *
     * @param batchId    batch
     * @param lineId     line
     * @param decision   new decision, or {@code null} to keep
     * @param memberId   member, or {@code null} to keep
     * @param categoryId category, or {@code null} to keep
     * @param aliasName  alias, or {@code null} to keep (blank removes it)
     * @return the updated line
     */
    @Transactional
    public ImportLine editLine(long batchId, long lineId, LineDecision decision, Long memberId, Long categoryId,
            String aliasName) {
        ImportBatch batch = queries.get(batchId);
        if (batch.status() != BatchStatus.PREVIEW) {
            throw new BusinessRuleException("import-not-in-preview", batchId);
        }
        ImportLine line = lines.findById(lineId).filter(l -> l.batchId() == batchId)
                .orElseThrow(() -> new NotFoundException("import-line", lineId));
        if (decision != null) {
            line.decide(decision);
        }
        line.edit(memberId, categoryId, aliasName);
        return lines.save(line);
    }

    /**
     * Discards a preview; nothing was written (RF-68).
     *
     * @param batchId batch
     * @return the cancelled batch
     */
    @Transactional
    public ImportBatch cancel(long batchId) {
        ImportBatch batch = queries.get(batchId);
        if (batch.status() != BatchStatus.PREVIEW) {
            throw new BusinessRuleException("import-not-in-preview", batchId);
        }
        batch.moveTo(BatchStatus.CANCELLED);
        return batches.save(batch);
    }

    /**
     * Undoes the latest confirmed batch of a card, removing its entries, including the pending ones (RF-72,
     * RF-79c, Q7).
     *
     * @param batchId batch
     * @return the undone batch
     * @throws BusinessRuleException with code {@code import-not-undoable} if it is not the latest confirmed
     *                               batch of its card
     */
    @Transactional
    public ImportBatch undo(long batchId) {
        ImportBatch batch = queries.get(batchId);
        if (!queries.isUndoable(batch)) {
            throw new BusinessRuleException("import-not-undoable", batchId);
        }
        ledger.undo(batchId);
        batch.moveTo(BatchStatus.UNDONE);
        return batches.save(batch);
    }
}

package com.planejamentomensal.imports.application;

import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.NewPurchase;
import com.planejamentomensal.entry.application.StoredEntry;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.imports.domain.LineDecision;
import com.planejamentomensal.imports.domain.LineStatus;
import com.planejamentomensal.imports.domain.RawLineKind;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.PurchaseFacts;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes a confirmed preview: new purchases with their projections, matched installments, updates of
 * divergent lines, IOF links, reconciliation of missing entries and the bank's future installments
 * (RF-72, RN-04 to RN-07, RN-20, RN-21, RF-63e).
 *
 * @since 1.0
 */
@Service
public class ConfirmImportUseCase {

    private final ImportQueries queries;
    private final ImportBatchRepository batches;
    private final ImportLineRepository lines;
    private final EntryLedger ledger;
    private final List<ConfirmationListener> listeners;

    /**
     * Creates the use case.
     *
     * @param queries   batch queries
     * @param batches   batch storage
     * @param lines     line storage
     * @param ledger    writer of entries
     * @param listeners actions after confirmation, such as storing aliases (RF-70)
     */
    public ConfirmImportUseCase(ImportQueries queries, ImportBatchRepository batches, ImportLineRepository lines,
            EntryLedger ledger, List<ConfirmationListener> listeners) {
        this.queries = queries;
        this.batches = batches;
        this.lines = lines;
        this.ledger = ledger;
        this.listeners = listeners;
    }

    /**
     * Confirms a batch in preview.
     *
     * @param batchId           batch
     * @param acknowledgeTotals {@code true} when the user reviewed a totals difference (RF-64h)
     * @return the confirmed batch
     * @throws BusinessRuleException with code {@code import-not-in-preview}, {@code import-totals-mismatch} or
     *                               {@code import-divergent-undecided}
     */
    @Transactional
    public ImportBatch confirm(long batchId, boolean acknowledgeTotals) {
        ImportBatch batch = queries.get(batchId);
        if (batch.status() != BatchStatus.PREVIEW) {
            throw new BusinessRuleException("import-not-in-preview", batchId);
        }
        if (acknowledgeTotals) {
            batch.acknowledgeTotals();
        }
        if (batch.totalsBlockConfirmation()) {
            throw new BusinessRuleException("import-totals-mismatch", batch.entriesTotal(), batch.computedTotal());
        }
        List<ImportLine> all = queries.lines(batchId);
        if (all.stream().anyMatch(l -> l.decision() == LineDecision.UNDECIDED)) {
            throw new BusinessRuleException("import-divergent-undecided", batchId);
        }
        boolean closed = !batch.openBill();
        ledger.recordBillFile(batchId, batch.cardId(), batch.billingPeriod(), batch.closingDate(), batch.dueDate(),
                batch.fileTotal(), closed, batch.nextClosingForecast());

        Set<Long> touched = new HashSet<>();
        List<ImportLine> real = all.stream().filter(l -> !l.futureCheck()).toList();
        for (ImportLine line : real) {
            if (line.matchedEntryId() != null) {
                touched.add(line.matchedEntryId());
            }
            switch (line.decision()) {
                case IMPORT -> {
                    if (line.status() == LineStatus.MATCHED) {
                        ledger.confirmProjected(batchId, line.matchedEntryId(), line.amount(), null);
                        line.recordEntry(line.matchedEntryId());
                    } else if (line.status() != LineStatus.DUPLICATE) {
                        line.recordEntry(ledger.recordPurchase(batchId, newPurchase(batch, line)));
                    }
                }
                case UPDATE_EXISTING -> {
                    ledger.overwrite(batchId, line.matchedEntryId(), line.amount(), null);
                    line.recordEntry(line.matchedEntryId());
                }
                default -> { }
            }
            if (line.createdEntryId() != null) {
                touched.add(line.createdEntryId());
            }
        }
        linkIof(real);
        List<Long> missing = ledger.entriesForMatching(batch.cardId()).stream()
                .filter(e -> e.period().equals(batch.billingPeriod()) && !touched.contains(e.entryId()))
                .filter(e -> closed && "PROJECTED".equals(e.status()) || importedEarlier(e, batchId))
                .map(StoredEntry::entryId)
                .toList();
        ledger.reconcileMissing(batchId, missing, batch.closingDate(), closed);
        if (closed) {
            ledger.applyFutureInstallments(batchId, batch.cardId(), all.stream().filter(ImportLine::futureCheck)
                    .map(l -> new PurchaseFacts(batch.cardId(), l.purchaseDate(), l.description(), l.installmentNumber(),
                            l.installmentCount(), l.amount()))
                    .toList(), batch.billingPeriod());
        }
        lines.saveAll(all);
        batch.moveTo(BatchStatus.CONFIRMED);
        ImportBatch saved = batches.save(batch);
        listeners.forEach(listener -> listener.confirmed(saved, real));
        return saved;
    }

    /** Confirmed by an earlier import (not by hand): may be missing from a newer file (RN-21, Q12). */
    private static boolean importedEarlier(StoredEntry entry, long batchId) {
        return "CONFIRMED".equals(entry.status()) && entry.importBatchId() != null && entry.importBatchId() != batchId;
    }

    private void linkIof(List<ImportLine> real) {
        for (ImportLine iof : real) {
            if (iof.kind() != RawLineKind.IOF || iof.relatedDescription() == null || iof.createdEntryId() == null) {
                continue;
            }
            real.stream()
                    .filter(l -> l != iof && l.createdEntryId() != null && l.purchaseDate().equals(iof.purchaseDate())
                            && EntryLedger.sameDescription(l.description(), iof.relatedDescription()))
                    .findFirst()
                    .ifPresent(target -> ledger.relate(iof.createdEntryId(), target.createdEntryId()));
        }
    }

    private static NewPurchase newPurchase(ImportBatch batch, ImportLine line) {
        String kind = switch (line.kind()) {
            case PURCHASE -> "PURCHASE";
            case CREDIT -> "CREDIT";
            case IOF -> "IOF";
            case FEE -> "FEE";
        };
        return new NewPurchase(batch.cardId(), line.cardLastDigits(), line.unknownFinal(), line.purchaseDate(),
                line.description(), line.installmentNumber(), line.installmentCount(), line.amount(), kind,
                line.bankCategory(), line.note(), batch.billingPeriod(), line.purchaseDate(), line.occurrence(),
                line.memberId() == null ? 0 : line.memberId(), line.categoryId(), false);
    }
}

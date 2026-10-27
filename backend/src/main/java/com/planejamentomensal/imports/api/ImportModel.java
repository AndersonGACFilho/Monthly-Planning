package com.planejamentomensal.imports.api;

import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.BillFormat;
import com.planejamentomensal.imports.domain.ImportBatch;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of an import batch and its preview lines (RF-68, RF-71, RF-72).
 *
 * @since 1.0
 */
@Relation(itemRelation = "import", collectionRelation = "imports")
public class ImportModel extends RepresentationModel<ImportModel> {

    /** Id. */
    public final long id;
    /** Primary card. */
    public final long cardId;
    /** Billing period chosen. */
    public final String billingPeriod;
    /** File format. */
    public final BillFormat format;
    /** File name. */
    public final String fileName;
    /** When the file was read. */
    public final Instant importedAt;
    /** Status. */
    public final BatchStatus status;
    /** Open (partial) bill. */
    public final boolean openBill;
    /** Due date read from the file. */
    public final LocalDate dueDate;
    /** Total printed on the file. */
    public final BigDecimal fileTotal;
    /** "Total dos lançamentos atuais". */
    public final BigDecimal entriesTotal;
    /** Sum of the lines read (RF-71). */
    public final BigDecimal computedTotal;
    /** Difference between printed and computed totals (RF-71). */
    public final BigDecimal totalDifference;
    /** Printed totals match the lines (RF-64h). */
    public final boolean totalsMatch;
    /** Warnings to confirm (RF-62, RF-73). */
    public final List<String> warnings;
    /** New lines. */
    public final int newCount;
    /** Duplicate lines. */
    public final int duplicateCount;
    /** Matched lines. */
    public final int matchedCount;
    /** Divergent lines. */
    public final int divergentCount;
    /** Lines, embedded only on the batch itself. */
    public final List<ImportLineModel> lines;

    /**
     * Copies a batch.
     *
     * @param batch batch
     * @param lines its lines, or an empty list in collections
     */
    public ImportModel(ImportBatch batch, List<ImportLineModel> lines) {
        this.id = batch.id();
        this.cardId = batch.cardId();
        this.billingPeriod = batch.billingPeriod().toString();
        this.format = batch.format();
        this.fileName = batch.fileName();
        this.importedAt = batch.importedAt();
        this.status = batch.status();
        this.openBill = batch.openBill();
        this.dueDate = batch.dueDate();
        this.fileTotal = batch.fileTotal() == null ? null : batch.fileTotal().amount();
        this.entriesTotal = batch.entriesTotal() == null ? null : batch.entriesTotal().amount();
        this.computedTotal = batch.computedTotal().amount();
        BigDecimal reference = batch.entriesTotal() != null ? batch.entriesTotal().amount()
                : batch.fileTotal() != null ? batch.fileTotal().amount() : batch.computedTotal().amount();
        this.totalDifference = reference.subtract(batch.computedTotal().amount());
        this.totalsMatch = batch.totalsMatch();
        this.warnings = batch.warnings();
        this.newCount = batch.newCount();
        this.duplicateCount = batch.duplicateCount();
        this.matchedCount = batch.matchedCount();
        this.divergentCount = batch.divergentCount();
        this.lines = lines;
    }
}

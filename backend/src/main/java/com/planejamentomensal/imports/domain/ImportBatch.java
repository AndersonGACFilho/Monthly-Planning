package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One import of a bill file: preview, confirmation and undo (RF-68, RF-72, RF-73).
 *
 * <p>Only the file hash and the summary are kept, never the file (RNF-04).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private long cardId;

    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillFormat format;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_hash", nullable = false)
    private String fileHash;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.PREVIEW;

    @Column(name = "open_bill", nullable = false)
    private boolean openBill;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "next_closing_forecast")
    private LocalDate nextClosingForecast;

    @Column(name = "card_last_digits")
    private String cardLastDigits;

    @Column(name = "file_total")
    private Money fileTotal;

    @Column(name = "entries_total")
    private Money entriesTotal;

    @Column(name = "computed_total", nullable = false)
    private Money computedTotal;

    @Column(name = "totals_match", nullable = false)
    private boolean totalsMatch;

    @Column(name = "totals_acknowledged", nullable = false)
    private boolean totalsAcknowledged;

    private String warnings;

    @Column(name = "new_count", nullable = false)
    private int newCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    @Column(name = "divergent_count", nullable = false)
    private int divergentCount;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected ImportBatch() {
    }

    /**
     * Creates a batch in preview.
     *
     * @param cardId        primary card
     * @param billingPeriod period chosen by the user (RN-03)
     * @param fileName      uploaded file name
     * @param fileHash      SHA-256 of the file (RF-73)
     * @param bill          what the file contains
     * @param warnings      warnings to confirm, such as a different due month (RF-62)
     */
    public ImportBatch(long cardId, BillingPeriod billingPeriod, String fileName, String fileHash, ParsedBill bill,
            List<String> warnings) {
        this.cardId = cardId;
        this.billingPeriod = billingPeriod;
        this.format = bill.format();
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.importedAt = Instant.now();
        this.openBill = bill.open();
        this.dueDate = bill.dueDate();
        this.closingDate = bill.closingDate();
        this.nextClosingForecast = bill.nextClosingForecast();
        this.cardLastDigits = bill.cardLastDigits();
        this.fileTotal = bill.fileTotal();
        this.entriesTotal = bill.entriesTotal();
        this.computedTotal = bill.computedTotal();
        this.totalsMatch = bill.totalsMatch();
        this.warnings = String.join(",", warnings);
    }

    /**
     * Records how many lines got each status (RF-72).
     *
     * @param lines preview lines
     */
    public void count(List<ImportLine> lines) {
        List<ImportLine> real = lines.stream().filter(l -> !l.futureCheck()).toList();
        newCount = (int) real.stream().filter(l -> l.status() == LineStatus.NEW).count();
        duplicateCount = (int) real.stream().filter(l -> l.status() == LineStatus.DUPLICATE).count();
        matchedCount = (int) real.stream().filter(l -> l.status() == LineStatus.MATCHED).count();
        divergentCount = (int) real.stream().filter(l -> l.status() == LineStatus.DIVERGENT).count();
    }

    /**
     * Records that the user reviewed a totals difference (RF-64h, RF-71).
     */
    public void acknowledgeTotals() {
        this.totalsAcknowledged = true;
    }

    /**
     * Tells whether confirmation is blocked by a totals difference (RF-64h).
     *
     * @return {@code true} if totals differ and were not acknowledged
     */
    public boolean totalsBlockConfirmation() {
        return !totalsMatch && !totalsAcknowledged;
    }

    /**
     * Moves the batch to a new state.
     *
     * @param next new state
     * @throws IllegalStateException for a transition that is not allowed
     */
    public void moveTo(BatchStatus next) {
        boolean allowed = switch (next) {
            case CONFIRMED, CANCELLED -> status == BatchStatus.PREVIEW;
            case UNDONE -> status == BatchStatus.CONFIRMED;
            case PREVIEW -> false;
        };
        if (!allowed) {
            throw new IllegalStateException(status + " -> " + next);
        }
        this.status = next;
    }

    /**
     * Returns the warnings (RF-62).
     *
     * @return warning codes
     */
    public List<String> warnings() {
        return warnings == null || warnings.isEmpty() ? List.of() : new ArrayList<>(Arrays.asList(warnings.split(",")));
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the primary card.
     *
     * @return card id
     */
    public long cardId() {
        return cardId;
    }

    /**
     * Returns the billing period chosen for the import.
     *
     * @return period
     */
    public BillingPeriod billingPeriod() {
        return billingPeriod;
    }

    /**
     * Returns the file format.
     *
     * @return format
     */
    public BillFormat format() {
        return format;
    }

    /**
     * Returns the uploaded file name.
     *
     * @return name
     */
    public String fileName() {
        return fileName;
    }

    /**
     * Returns the file hash.
     *
     * @return SHA-256 hex
     */
    public String fileHash() {
        return fileHash;
    }

    /**
     * Returns when the file was read.
     *
     * @return instant
     */
    public Instant importedAt() {
        return importedAt;
    }

    /**
     * Returns the status.
     *
     * @return status
     */
    public BatchStatus status() {
        return status;
    }

    /**
     * Tells whether the file is an open (partial) bill (RF-63f).
     *
     * @return flag
     */
    public boolean openBill() {
        return openBill;
    }

    /**
     * Returns the due date read from the file.
     *
     * @return date
     */
    public LocalDate dueDate() {
        return dueDate;
    }

    /**
     * Returns the closing date read from the file.
     *
     * @return date, or {@code null}
     */
    public LocalDate closingDate() {
        return closingDate;
    }

    /**
     * Returns the next closing forecast (RN-22).
     *
     * @return date, or {@code null}
     */
    public LocalDate nextClosingForecast() {
        return nextClosingForecast;
    }

    /**
     * Returns the card final in the file header.
     *
     * @return final, or {@code null}
     */
    public String cardLastDigits() {
        return cardLastDigits;
    }

    /**
     * Returns the total printed on the file.
     *
     * @return total, or {@code null}
     */
    public Money fileTotal() {
        return fileTotal;
    }

    /**
     * Returns "Total dos lançamentos atuais".
     *
     * @return total, or {@code null}
     */
    public Money entriesTotal() {
        return entriesTotal;
    }

    /**
     * Returns the sum of the lines read (RF-71).
     *
     * @return computed total
     */
    public Money computedTotal() {
        return computedTotal;
    }

    /**
     * Tells whether printed and computed totals match (RF-64h).
     *
     * @return flag
     */
    public boolean totalsMatch() {
        return totalsMatch;
    }

    /**
     * Tells whether the user acknowledged a totals difference.
     *
     * @return flag
     */
    public boolean totalsAcknowledged() {
        return totalsAcknowledged;
    }

    /**
     * Returns the number of new lines.
     *
     * @return count
     */
    public int newCount() {
        return newCount;
    }

    /**
     * Returns the number of duplicate lines.
     *
     * @return count
     */
    public int duplicateCount() {
        return duplicateCount;
    }

    /**
     * Returns the number of matched lines.
     *
     * @return count
     */
    public int matchedCount() {
        return matchedCount;
    }

    /**
     * Returns the number of divergent lines.
     *
     * @return count
     */
    public int divergentCount() {
        return divergentCount;
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}

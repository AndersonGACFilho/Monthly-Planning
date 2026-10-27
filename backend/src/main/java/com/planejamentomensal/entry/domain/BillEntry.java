package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Auditable;
import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One installment, single purchase or subscription charge on one card bill (glossary: lançamento).
 *
 * @since 1.0
 */
@Entity
@Table(name = "bill_entry")
public class BillEntry implements Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "card_bill_id")
    private CardBill cardBill;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(nullable = false)
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryStatus status;

    @Column(name = "installment_key", nullable = false)
    private String installmentKey;

    @Column(name = "related_entry_id")
    private Long relatedEntryId;

    @Column(name = "import_batch_id")
    private Long importBatchId;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected BillEntry() {
    }

    /**
     * Creates an entry.
     *
     * @param purchase          purchase it belongs to
     * @param cardBill          bill it appears on
     * @param installmentNumber installment number, or {@code null} for single purchases and subscriptions
     * @param entryDate         date on the bill
     * @param amount            amount
     * @param status            projected or confirmed
     * @param installmentKey    key of RN-10
     * @param importBatchId     batch that created it, or {@code null} for manual entries
     */
    public BillEntry(Purchase purchase, CardBill cardBill, Integer installmentNumber, LocalDate entryDate, Money amount,
            EntryStatus status, String installmentKey, Long importBatchId) {
        this.purchase = Objects.requireNonNull(purchase, "purchase");
        this.cardBill = Objects.requireNonNull(cardBill, "cardBill");
        this.installmentNumber = installmentNumber;
        this.entryDate = entryDate;
        this.amount = amount;
        this.status = status;
        this.installmentKey = installmentKey;
        this.importBatchId = importBatchId;
    }

    /**
     * Confirms a projected entry seen on an imported bill, adjusting amount and date (RN-06).
     *
     * @param seenAmount amount on the bill
     * @param seenDate   date on the bill, or {@code null} to keep the projected date
     * @param batchId    batch that confirmed it
     */
    public void confirm(Money seenAmount, LocalDate seenDate, Long batchId) {
        this.amount = seenAmount;
        if (seenDate != null) {
            this.entryDate = seenDate;
        }
        this.status = EntryStatus.CONFIRMED;
        this.importBatchId = batchId;
    }

    /**
     * Confirms a projected entry by hand (API link {@code confirm}).
     */
    public void confirmManually() {
        this.status = EntryStatus.CONFIRMED;
    }

    /**
     * Marks a projected entry missing from an imported bill (RN-07, RN-21).
     */
    public void markUnconfirmed() {
        this.status = EntryStatus.UNCONFIRMED;
    }

    /**
     * Restores state and values, used to undo a batch (RF-72).
     *
     * @param previousStatus  status before
     * @param previousAmount  amount before
     * @param previousDate    date before
     * @param previousBill    bill before
     * @param previousBatchId batch before
     */
    public void restore(EntryStatus previousStatus, Money previousAmount, LocalDate previousDate, CardBill previousBill,
            Long previousBatchId) {
        this.status = previousStatus;
        this.amount = previousAmount;
        this.entryDate = previousDate;
        this.cardBill = previousBill;
        this.importBatchId = previousBatchId;
    }

    /**
     * Moves the entry to another bill, such as the next one when a partial entry is after the closing date
     * (RN-21).
     *
     * @param bill new bill
     */
    public void moveTo(CardBill bill) {
        this.cardBill = bill;
    }

    /**
     * Updates amount from the bank's list of future installments (RN-20).
     *
     * @param bankAmount amount announced by the bank
     */
    public void adjustAmount(Money bankAmount) {
        this.amount = bankAmount;
    }

    /**
     * Links an IOF entry to the purchase it was charged for (RF-63e).
     *
     * @param entryId entry of the international purchase
     */
    public void relateTo(Long entryId) {
        this.relatedEntryId = entryId;
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
     * Returns the purchase.
     *
     * @return purchase
     */
    public Purchase purchase() {
        return purchase;
    }

    /**
     * Returns the bill.
     *
     * @return card bill
     */
    public CardBill cardBill() {
        return cardBill;
    }

    /**
     * Returns the installment number.
     *
     * @return number, or {@code null}
     */
    public Integer installmentNumber() {
        return installmentNumber;
    }

    /**
     * Returns the installment as shown: {@code n/N}, {@code -} or {@code *}.
     *
     * @return label
     */
    public String installmentLabel() {
        if (purchase.type() == PurchaseType.SUBSCRIPTION) {
            return "*";
        }
        return installmentNumber == null ? "-" : "%02d/%02d".formatted(installmentNumber, purchase.installmentCount());
    }

    /**
     * Returns the date on the bill.
     *
     * @return entry date
     */
    public LocalDate entryDate() {
        return entryDate;
    }

    /**
     * Returns the amount; negative for refunds and credits.
     *
     * @return amount
     */
    public Money amount() {
        return amount;
    }

    /**
     * Returns the status.
     *
     * @return projected, confirmed or unconfirmed
     */
    public EntryStatus status() {
        return status;
    }

    /**
     * Returns the installment key (RN-10).
     *
     * @return key
     */
    public String installmentKey() {
        return installmentKey;
    }

    /**
     * Returns the entry of the purchase an IOF line belongs to.
     *
     * @return entry id, or {@code null}
     */
    public Long relatedEntryId() {
        return relatedEntryId;
    }

    /**
     * Returns the batch that created or last confirmed the entry.
     *
     * @return batch id, or {@code null}
     */
    public Long importBatchId() {
        return importBatchId;
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }

    /**
     * Name in the audit log (RNF-05).
     *
     * @return {@code bill-entry}
     */
    @Override
    public String auditType() {
        return "bill-entry";
    }

    /**
     * Only changes of confirmed entries are audited (RNF-05).
     *
     * @param previousStatus status before the change
     * @return {@code true} if the entry was confirmed
     */
    @Override
    public boolean audits(Object previousStatus) {
        return previousStatus == EntryStatus.CONFIRMED;
    }
}

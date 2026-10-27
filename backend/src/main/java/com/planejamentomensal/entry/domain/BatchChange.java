package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * One change an import batch made, with the values before it, so the batch can be undone exactly (RF-72).
 *
 * @since 1.0
 */
@Entity
@Table(name = "batch_change")
public class BatchChange {

    /** What the batch did. */
    public enum Type {
        /** Created a purchase. */
        PURCHASE_CREATED,
        /** Created an entry (confirmed or projected). */
        ENTRY_CREATED,
        /** Changed an existing entry (confirmed, unconfirmed, moved or adjusted). */
        ENTRY_CHANGED,
        /** Removed a projected entry, such as remaining installments of a refunded purchase (RN-29). */
        ENTRY_REMOVED,
        /** Registered a refund on an earlier purchase (RN-26). */
        REFUND_REGISTERED,
        /** Changed a bill's dates, total or status. */
        BILL_CHANGED,
        /** Created a bill. */
        BILL_CREATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private long batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private Type type;

    @Column(name = "target_id", nullable = false)
    private long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private EntryStatus previousStatus;

    @Column(name = "previous_amount")
    private Money previousAmount;

    @Column(name = "previous_date")
    private LocalDate previousDate;

    @Column(name = "previous_bill_id")
    private Long previousBillId;

    @Column(name = "previous_batch_id")
    private Long previousBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_bill_status")
    private BillStatus previousBillStatus;

    @Column(name = "previous_partial")
    private Boolean previousPartial;

    @Column(name = "previous_closing")
    private LocalDate previousClosing;

    @Column(name = "previous_due")
    private LocalDate previousDue;

    /** Removed projected entry: purchase it belonged to. */
    @Column(name = "removed_purchase_id")
    private Long removedPurchaseId;

    /** Removed projected entry: installment number and key, to recreate it. */
    @Column(name = "removed_installment")
    private Integer removedInstallment;

    @Column(name = "removed_key")
    private String removedKey;

    /**
     * For JPA.
     */
    protected BatchChange() {
    }

    private BatchChange(long batchId, Type type, long targetId) {
        this.batchId = batchId;
        this.type = type;
        this.targetId = targetId;
    }

    /**
     * Records a created purchase, entry or bill.
     *
     * @param batchId  batch
     * @param type     PURCHASE_CREATED, ENTRY_CREATED or BILL_CREATED
     * @param targetId created row
     * @return the change
     */
    public static BatchChange created(long batchId, Type type, long targetId) {
        return new BatchChange(batchId, type, targetId);
    }

    /**
     * Records the state of an entry before the batch changed it.
     *
     * @param batchId batch
     * @param entry   entry before the change
     * @return the change
     */
    public static BatchChange entryChanged(long batchId, BillEntry entry) {
        BatchChange change = new BatchChange(batchId, Type.ENTRY_CHANGED, entry.id());
        change.previousStatus = entry.status();
        change.previousAmount = entry.amount();
        change.previousDate = entry.entryDate();
        change.previousBillId = entry.cardBill().id();
        change.previousBatchId = entry.importBatchId();
        return change;
    }

    /**
     * Records a removed projected entry, with what is needed to recreate it.
     *
     * @param batchId batch
     * @param entry   removed entry
     * @return the change
     */
    public static BatchChange entryRemoved(long batchId, BillEntry entry) {
        BatchChange change = new BatchChange(batchId, Type.ENTRY_REMOVED, entry.id());
        change.previousStatus = entry.status();
        change.previousAmount = entry.amount();
        change.previousDate = entry.entryDate();
        change.previousBillId = entry.cardBill().id();
        change.previousBatchId = entry.importBatchId();
        change.removedPurchaseId = entry.purchase().id();
        change.removedInstallment = entry.installmentNumber();
        change.removedKey = entry.installmentKey();
        return change;
    }

    /**
     * Records a refund registered on a purchase.
     *
     * @param batchId    batch
     * @param purchaseId refunded purchase
     * @param amount     refunded amount
     * @return the change
     */
    public static BatchChange refundRegistered(long batchId, long purchaseId, Money amount) {
        BatchChange change = new BatchChange(batchId, Type.REFUND_REGISTERED, purchaseId);
        change.previousAmount = amount;
        return change;
    }

    /**
     * Records the state of a bill before the batch changed it.
     *
     * @param batchId batch
     * @param bill    bill before the change
     * @return the change
     */
    public static BatchChange billChanged(long batchId, CardBill bill) {
        BatchChange change = new BatchChange(batchId, Type.BILL_CHANGED, bill.id());
        change.previousBillStatus = bill.status();
        change.previousPartial = bill.partial();
        change.previousClosing = bill.closingDate();
        change.previousDue = bill.dueDate();
        change.previousAmount = bill.fileTotal();
        return change;
    }

    /**
     * Returns the id; later changes have larger ids and are undone first.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the batch.
     *
     * @return batch id
     */
    public long batchId() {
        return batchId;
    }

    /**
     * Returns what was done.
     *
     * @return type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the changed row.
     *
     * @return id of the purchase, entry or bill
     */
    public long targetId() {
        return targetId;
    }

    /**
     * Returns the previous entry status.
     *
     * @return status
     */
    public EntryStatus previousStatus() {
        return previousStatus;
    }

    /**
     * Returns the previous amount (entry amount, bill total or refunded amount).
     *
     * @return amount
     */
    public Money previousAmount() {
        return previousAmount;
    }

    /**
     * Returns the previous entry date.
     *
     * @return date
     */
    public LocalDate previousDate() {
        return previousDate;
    }

    /**
     * Returns the previous bill of an entry.
     *
     * @return bill id
     */
    public Long previousBillId() {
        return previousBillId;
    }

    /**
     * Returns the previous batch of an entry.
     *
     * @return batch id
     */
    public Long previousBatchId() {
        return previousBatchId;
    }

    /**
     * Returns the previous bill status.
     *
     * @return status
     */
    public BillStatus previousBillStatus() {
        return previousBillStatus;
    }

    /**
     * Returns the previous partial flag of a bill.
     *
     * @return flag
     */
    public Boolean previousPartial() {
        return previousPartial;
    }

    /**
     * Returns the previous closing date of a bill.
     *
     * @return date
     */
    public LocalDate previousClosing() {
        return previousClosing;
    }

    /**
     * Returns the previous due date of a bill.
     *
     * @return date
     */
    public LocalDate previousDue() {
        return previousDue;
    }

    /**
     * Returns the purchase of a removed entry.
     *
     * @return purchase id
     */
    public Long removedPurchaseId() {
        return removedPurchaseId;
    }

    /**
     * Returns the installment number of a removed entry.
     *
     * @return number
     */
    public Integer removedInstallment() {
        return removedInstallment;
    }

    /**
     * Returns the key of a removed entry.
     *
     * @return key
     */
    public String removedKey() {
        return removedKey;
    }
}

package com.planejamentomensal.entry.domain;

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
import java.time.LocalDate;

/**
 * The bill of a (primary) card in one billing period (glossary: fatura).
 *
 * <p>Holds the closing and due dates read from files, which win over the card's typical days (RN-22), and
 * the total printed on the file for checking (RF-71).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "card_bill")
public class CardBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", nullable = false)
    private long cardId;

    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(name = "closing_date")
    private LocalDate closingDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "file_total")
    private Money fileTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status = BillStatus.OPEN;

    @Column(nullable = false)
    private boolean partial;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected CardBill() {
    }

    /**
     * Creates an open bill.
     *
     * @param cardId        primary card
     * @param billingPeriod billing period
     */
    public CardBill(long cardId, BillingPeriod billingPeriod) {
        this.cardId = cardId;
        this.billingPeriod = billingPeriod;
    }

    /**
     * Records what an imported file says about the bill.
     *
     * @param closing   closing date, or {@code null} to keep the current one
     * @param due       due date, or {@code null}
     * @param total     total printed on the file, or {@code null}
     * @param closed    {@code true} for a closed bill (PDF)
     */
    public void recordFile(LocalDate closing, LocalDate due, Money total, boolean closed) {
        if (closing != null) {
            this.closingDate = closing;
        }
        if (due != null) {
            this.dueDate = due;
        }
        if (total != null) {
            this.fileTotal = total;
        }
        if (closed) {
            this.status = status == BillStatus.PAID ? BillStatus.PAID : BillStatus.CLOSED;
            this.partial = false;
        } else if (status == BillStatus.OPEN) {
            this.partial = true;
        }
    }

    /**
     * Sets the closing date known from the previous bill's "next closing forecast" (RN-22).
     *
     * @param forecast forecast closing date
     */
    public void forecastClosing(LocalDate forecast) {
        if (status == BillStatus.OPEN && forecast != null) {
            this.closingDate = forecast;
        }
    }

    /**
     * Marks a closed bill as paid (RF-36).
     *
     * @throws IllegalStateException if the bill is not closed
     */
    public void markPaid() {
        if (status != BillStatus.CLOSED) {
            throw new IllegalStateException("Only closed bills can be paid");
        }
        status = BillStatus.PAID;
    }

    /**
     * Restores the state before an undone import (RF-72).
     *
     * @param previousStatus  status before
     * @param previousPartial partial flag before
     * @param previousClosing closing date before
     * @param previousDue     due date before
     * @param previousTotal   file total before
     */
    public void restore(BillStatus previousStatus, boolean previousPartial, LocalDate previousClosing,
            LocalDate previousDue, Money previousTotal) {
        this.status = previousStatus;
        this.partial = previousPartial;
        this.closingDate = previousClosing;
        this.dueDate = previousDue;
        this.fileTotal = previousTotal;
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
     * Returns the billing period.
     *
     * @return period
     */
    public BillingPeriod billingPeriod() {
        return billingPeriod;
    }

    /**
     * Returns the closing date.
     *
     * @return closing date, or {@code null} when unknown
     */
    public LocalDate closingDate() {
        return closingDate;
    }

    /**
     * Returns the due date.
     *
     * @return due date, or {@code null} when unknown
     */
    public LocalDate dueDate() {
        return dueDate;
    }

    /**
     * Returns the total printed on the imported file.
     *
     * @return total, or {@code null}
     */
    public Money fileTotal() {
        return fileTotal;
    }

    /**
     * Returns the status.
     *
     * @return status
     */
    public BillStatus status() {
        return status;
    }

    /**
     * Tells whether only an open (partial) file was imported (RF-63f).
     *
     * @return partial flag
     */
    public boolean partial() {
        return partial;
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

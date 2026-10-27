package com.planejamentomensal.recurrence.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
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

/**
 * The income or fixed expense of a rule in one billing period (RF-22, RF-24, RF-31, RF-32, RF-33).
 *
 * @since 1.0
 */
@Entity
@Table(name = "occurrence")
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "recurrence_rule_id")
    private RecurrenceRule rule;

    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(nullable = false)
    private Money amount;

    @Column(name = "amount_overridden", nullable = false)
    private boolean amountOverridden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccurrenceStatus status = OccurrenceStatus.EXPECTED;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "linked_entry_id")
    private Long linkedEntryId;

    @Column(name = "link_candidates")
    private String linkCandidates;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Occurrence() {
    }

    /**
     * Generates the occurrence of a rule with its default amount (RF-22, RF-31).
     *
     * @param rule   rule
     * @param period billing period
     */
    public Occurrence(RecurrenceRule rule, BillingPeriod period) {
        this.rule = rule;
        this.billingPeriod = period;
        this.amount = rule.defaultAmount();
    }

    /**
     * Changes the amount of this period only; the rule keeps its default (RF-22, RF-31).
     *
     * @param newAmount amount of this period
     */
    public void overrideAmount(Money newAmount) {
        this.amount = newAmount;
        this.amountOverridden = true;
    }

    /**
     * Follows a new default amount of the rule unless this period was changed by hand.
     *
     * @param defaultAmount the rule's default amount
     */
    public void followDefault(Money defaultAmount) {
        if (!amountOverridden && status == OccurrenceStatus.EXPECTED) {
            this.amount = defaultAmount;
        }
    }

    /**
     * Marks the income as received or the expense as paid (RF-24, RF-32).
     *
     * @param date receipt or payment date
     */
    public void markDone(LocalDate date) {
        this.status = OccurrenceStatus.DONE;
        this.effectiveDate = date;
    }

    /**
     * Marks the income as expected or the expense as pending again.
     */
    public void markExpected() {
        this.status = OccurrenceStatus.EXPECTED;
        this.effectiveDate = null;
    }

    /**
     * Links a fixed expense paid by card to the card entry that holds it (RF-33).
     *
     * @param entryId entry
     * @param date    entry date
     */
    public void linkTo(long entryId, LocalDate date) {
        this.linkedEntryId = entryId;
        this.linkCandidates = null;
        markDone(date);
    }

    /**
     * Records several possible entries; the user chooses one (RN-30).
     *
     * @param candidates comma-separated entry ids
     */
    public void offerCandidates(String candidates) {
        this.linkCandidates = candidates;
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
     * Returns the rule.
     *
     * @return rule
     */
    public RecurrenceRule rule() {
        return rule;
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
     * Returns the amount of this period.
     *
     * @return amount
     */
    public Money amount() {
        return amount;
    }

    /**
     * Tells whether the amount was changed for this period only.
     *
     * @return flag
     */
    public boolean amountOverridden() {
        return amountOverridden;
    }

    /**
     * Returns the status.
     *
     * @return expected or done
     */
    public OccurrenceStatus status() {
        return status;
    }

    /**
     * Returns the receipt or payment date.
     *
     * @return date, or {@code null}
     */
    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    /**
     * Returns the card entry that holds this expense (RF-33).
     *
     * @return entry id, or {@code null}
     */
    public Long linkedEntryId() {
        return linkedEntryId;
    }

    /**
     * Returns the candidate entries waiting for the user's choice (RN-30).
     *
     * @return comma-separated ids, or {@code null}
     */
    public String linkCandidates() {
        return linkCandidates;
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

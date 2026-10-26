package com.planejamentomensal.card.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Closing and due day of a card, valid from a billing period on (RF-04).
 *
 * @since 1.0
 */
@Entity
@Table(name = "card_cycle")
public class CardCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "closing_day", nullable = false)
    private int closingDay;

    @Column(name = "due_day", nullable = false)
    private int dueDay;

    @Column(name = "valid_from", nullable = false)
    private BillingPeriod validFrom;

    /**
     * For JPA.
     */
    protected CardCycle() {
    }

    /**
     * Creates a cycle.
     *
     * @param closingDay day the bill closes, 1 to 31
     * @param dueDay     day the bill is due, 1 to 31
     * @param validFrom  first billing period that uses this cycle
     * @throws IllegalArgumentException if a day is outside 1 to 31
     */
    public CardCycle(int closingDay, int dueDay, BillingPeriod validFrom) {
        if (closingDay < 1 || closingDay > 31 || dueDay < 1 || dueDay > 31) {
            throw new IllegalArgumentException("Closing and due days must be between 1 and 31");
        }
        this.closingDay = closingDay;
        this.dueDay = dueDay;
        this.validFrom = validFrom;
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
     * Returns the closing day.
     *
     * @return day of month
     */
    public int closingDay() {
        return closingDay;
    }

    /**
     * Returns the due day.
     *
     * @return day of month
     */
    public int dueDay() {
        return dueDay;
    }

    /**
     * Returns the first period using this cycle.
     *
     * @return billing period
     */
    public BillingPeriod validFrom() {
        return validFrom;
    }
}

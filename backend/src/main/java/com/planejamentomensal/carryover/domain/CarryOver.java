package com.planejamentomensal.carryover.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * What a third-party member still owed at the end of a period, owed again in the next one (RF-28). The entries stay
 * in their own period, so each period keeps its own total.
 *
 * @since 1.0
 */
@Entity
@Table(name = "carry_over")
public class CarryOver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "from_period", nullable = false)
    private BillingPeriod fromPeriod;

    @Column(name = "to_period", nullable = false)
    private BillingPeriod toPeriod;

    @Column(nullable = false)
    private Money amount;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected CarryOver() {
    }

    /**
     * Carries an amount from a period into the next one.
     *
     * @param memberId   member who owes
     * @param fromPeriod period that was not paid in full
     * @param amount     amount still owed
     */
    public CarryOver(long memberId, BillingPeriod fromPeriod, Money amount) {
        this.memberId = memberId;
        this.fromPeriod = fromPeriod;
        this.toPeriod = fromPeriod.next();
        changeAmount(amount);
    }

    /**
     * Replaces the amount, after the period changed.
     *
     * @param amount amount still owed
     * @throws BusinessRuleException with code {@code carry-over-nothing-due} when the amount is not positive
     */
    public void changeAmount(Money amount) {
        if (amount.isNegative() || amount.isZero()) {
            throw new BusinessRuleException("carry-over-nothing-due", fromPeriod);
        }
        this.amount = amount;
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
     * Returns the member.
     *
     * @return member id
     */
    public long memberId() {
        return memberId;
    }

    /**
     * Returns the period the amount comes from.
     *
     * @return source period
     */
    public BillingPeriod fromPeriod() {
        return fromPeriod;
    }

    /**
     * Returns the period the amount is owed in.
     *
     * @return the period after the source
     */
    public BillingPeriod toPeriod() {
        return toPeriod;
    }

    /**
     * Returns the amount.
     *
     * @return positive amount
     */
    public Money amount() {
        return amount;
    }
}

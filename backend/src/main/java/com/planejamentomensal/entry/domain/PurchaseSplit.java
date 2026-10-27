package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Share of a purchase assigned to a member, by percentage or by amount (RF-26).
 *
 * @since 1.0
 */
@Entity
@Table(name = "purchase_split")
public class PurchaseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    private BigDecimal percentage;

    private Money amount;

    /**
     * For JPA.
     */
    protected PurchaseSplit() {
    }

    /**
     * Creates a share.
     *
     * @param memberId   member
     * @param percentage share in percent, or {@code null} when given by amount
     * @param amount     share per installment in reais, or {@code null} when given by percentage
     */
    public PurchaseSplit(long memberId, BigDecimal percentage, Money amount) {
        if ((percentage == null) == (amount == null)) {
            throw new IllegalArgumentException("A split has either a percentage or an amount");
        }
        this.memberId = memberId;
        this.percentage = percentage;
        this.amount = amount;
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
     * Returns the percentage.
     *
     * @return percentage, or {@code null}
     */
    public BigDecimal percentage() {
        return percentage;
    }

    /**
     * Returns the fixed amount per installment.
     *
     * @return amount, or {@code null}
     */
    public Money amount() {
        return amount;
    }
}

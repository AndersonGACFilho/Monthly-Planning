package com.planejamentomensal.recurrence.domain;

import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
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

/**
 * Parameterized income or fixed expense: registered once, generated in every billing period it applies to
 * (RF-21, RF-22, RF-30, RF-31). Incomes and fixed expenses differ only by {@link RuleKind} (DRY).
 *
 * @since 1.0
 */
@Entity
@Table(name = "recurrence_rule")
public class RecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleKind kind;

    @Column(nullable = false)
    private String description;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type")
    private IncomeType incomeType;

    @Column(name = "default_amount", nullable = false)
    private Money defaultAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence;

    @Column(name = "interval_months", nullable = false)
    private int intervalMonths;

    @Column(nullable = false)
    private int day;

    @Column(name = "valid_from", nullable = false)
    private BillingPeriod validFrom;

    @Column(name = "valid_until")
    private BillingPeriod validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected RecurrenceRule() {
    }

    /**
     * Creates a rule.
     *
     * @param kind    income or fixed expense
     * @param details rule data
     */
    public RecurrenceRule(RuleKind kind, RuleDetails details) {
        this.kind = kind;
        update(details);
    }

    /**
     * Replaces the rule data; occurrences already generated keep their amounts (RF-22).
     *
     * @param details new data
     * @throws InvalidInputException with code {@code rule-day-invalid}, {@code rule-interval-invalid} or
     *         {@code rule-validity-invalid}
     */
    public void update(RuleDetails details) {
        if (details.day() < 1 || details.day() > 31) {
            throw new InvalidInputException("rule-day-invalid", details.day());
        }
        if (details.recurrence() == Recurrence.EVERY_N_MONTHS && details.intervalMonths() < 2) {
            throw new InvalidInputException("rule-interval-invalid", details.intervalMonths());
        }
        if (details.validUntil() != null && details.validUntil().compareTo(details.validFrom()) < 0) {
            throw new InvalidInputException("rule-validity-invalid", details.validFrom(), details.validUntil());
        }
        this.description = details.description();
        this.memberId = details.memberId();
        this.categoryId = details.categoryId();
        this.incomeType = kind == RuleKind.INCOME ? details.incomeType() : null;
        this.defaultAmount = details.defaultAmount();
        this.recurrence = details.recurrence();
        this.intervalMonths = details.recurrence() == Recurrence.EVERY_N_MONTHS ? details.intervalMonths() : 1;
        this.day = details.day();
        this.validFrom = details.validFrom();
        this.validUntil = details.recurrence() == Recurrence.ONCE ? details.validFrom() : details.validUntil();
        this.paymentMethod = kind == RuleKind.FIXED_EXPENSE ? details.paymentMethod() : null;
    }

    /**
     * Tells whether the rule generates an occurrence in a billing period (RF-22, RF-31).
     *
     * @param period billing period
     * @return {@code true} if active, valid in the period and due by its recurrence
     */
    public boolean appliesTo(BillingPeriod period) {
        if (!active || period.compareTo(validFrom) < 0 || validUntil != null && period.compareTo(validUntil) > 0) {
            return false;
        }
        return switch (recurrence) {
            case MONTHLY -> true;
            case ONCE -> period.equals(validFrom);
            case EVERY_N_MONTHS -> validFrom.monthsUntil(period) % intervalMonths == 0;
        };
    }

    /**
     * Activates or inactivates the rule; inactive rules stop generating occurrences.
     *
     * @param newActive new state
     */
    public void setActive(boolean newActive) {
        this.active = newActive;
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
     * Returns the kind.
     *
     * @return income or fixed expense
     */
    public RuleKind kind() {
        return kind;
    }

    /**
     * Returns the description.
     *
     * @return description
     */
    public String description() {
        return description;
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
     * Returns the category.
     *
     * @return category id, or {@code null}
     */
    public Long categoryId() {
        return categoryId;
    }

    /**
     * Returns the kind of income.
     *
     * @return income type, or {@code null} for expenses
     */
    public IncomeType incomeType() {
        return incomeType;
    }

    /**
     * Returns the default amount.
     *
     * @return amount
     */
    public Money defaultAmount() {
        return defaultAmount;
    }

    /**
     * Returns the recurrence.
     *
     * @return recurrence
     */
    public Recurrence recurrence() {
        return recurrence;
    }

    /**
     * Returns N of "every N months".
     *
     * @return interval
     */
    public int intervalMonths() {
        return intervalMonths;
    }

    /**
     * Returns the expected or due day.
     *
     * @return day of month
     */
    public int day() {
        return day;
    }

    /**
     * Returns the first period.
     *
     * @return period
     */
    public BillingPeriod validFrom() {
        return validFrom;
    }

    /**
     * Returns the last period.
     *
     * @return period, or {@code null}
     */
    public BillingPeriod validUntil() {
        return validUntil;
    }

    /**
     * Returns the payment method.
     *
     * @return method, or {@code null} for incomes
     */
    public PaymentMethod paymentMethod() {
        return paymentMethod;
    }

    /**
     * Tells whether the rule is active.
     *
     * @return active flag
     */
    public boolean active() {
        return active;
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

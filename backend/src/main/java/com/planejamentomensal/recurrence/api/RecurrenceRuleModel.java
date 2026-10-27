package com.planejamentomensal.recurrence.api;

import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleKind;
import java.math.BigDecimal;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of an income or fixed expense rule.
 *
 * @since 1.0
 */
@Relation(itemRelation = "recurrenceRule", collectionRelation = "recurrenceRules")
public class RecurrenceRuleModel extends RepresentationModel<RecurrenceRuleModel> {

    /** Id. */
    public final long id;
    /** Income or fixed expense. */
    public final RuleKind kind;
    /** Description. */
    public final String description;
    /** Member. */
    public final long memberId;
    /** Category, or {@code null}. */
    public final Long categoryId;
    /** Income type; {@code null} for fixed expenses. */
    public final IncomeType incomeType;
    /** Default amount. */
    public final BigDecimal defaultAmount;
    /** Recurrence. */
    public final Recurrence recurrence;
    /** N of every N months; 1 otherwise. */
    public final int intervalMonths;
    /** Expected day or due day. */
    public final int day;
    /** First billing period. */
    public final String validFrom;
    /** Last billing period, or {@code null}. */
    public final String validUntil;
    /** Payment method; {@code null} for incomes. */
    public final PaymentMethod paymentMethod;
    /** Active flag. */
    public final boolean active;
    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies a rule.
     *
     * @param rule rule
     */
    public RecurrenceRuleModel(RecurrenceRule rule) {
        this.id = rule.id();
        this.kind = rule.kind();
        this.description = rule.description();
        this.memberId = rule.memberId();
        this.categoryId = rule.categoryId();
        this.incomeType = rule.incomeType();
        this.defaultAmount = rule.defaultAmount().amount();
        this.recurrence = rule.recurrence();
        this.intervalMonths = rule.intervalMonths();
        this.day = rule.day();
        this.validFrom = rule.validFrom().toString();
        this.validUntil = rule.validUntil() == null ? null : rule.validUntil().toString();
        this.paymentMethod = rule.paymentMethod();
        this.active = rule.active();
        this.version = rule.version();
    }
}

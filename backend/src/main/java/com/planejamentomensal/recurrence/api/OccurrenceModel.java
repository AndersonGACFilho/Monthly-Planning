package com.planejamentomensal.recurrence.api;

import com.planejamentomensal.recurrence.domain.IncomeType;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.recurrence.domain.RuleKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of the income or fixed expense of one billing period.
 *
 * @since 1.0
 */
@Relation(itemRelation = "occurrence", collectionRelation = "occurrences")
public class OccurrenceModel extends RepresentationModel<OccurrenceModel> {

    /** Id. */
    public final long id;
    /** Rule. */
    public final long ruleId;
    /** Income or fixed expense. */
    public final RuleKind kind;
    /** Description of the rule. */
    public final String description;
    /** Member. */
    public final long memberId;
    /** Category, or {@code null}. */
    public final Long categoryId;
    /** Income type; {@code null} for fixed expenses. */
    public final IncomeType incomeType;
    /** Payment method; {@code null} for incomes. */
    public final PaymentMethod paymentMethod;
    /** Billing period. */
    public final String billingPeriod;
    /** Expected or due day. */
    public final int day;
    /** Amount of the period. */
    public final BigDecimal amount;
    /** Default amount of the rule. */
    public final BigDecimal defaultAmount;
    /** {@code true} when the amount was changed for this period only. */
    public final boolean amountOverridden;
    /** Expected or done (received or paid). */
    public final OccurrenceStatus status;
    /** Receipt or payment date. */
    public final LocalDate effectiveDate;
    /** Card entry that holds a card-paid expense (RF-33). */
    public final Long linkedEntryId;
    /** Candidate entries waiting for the user's choice (RN-30). */
    public final List<Long> linkCandidates;
    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies an occurrence.
     *
     * @param occurrence occurrence
     */
    public OccurrenceModel(Occurrence occurrence) {
        this.id = occurrence.id();
        this.ruleId = occurrence.rule().id();
        this.kind = occurrence.rule().kind();
        this.description = occurrence.rule().description();
        this.memberId = occurrence.rule().memberId();
        this.categoryId = occurrence.rule().categoryId();
        this.incomeType = occurrence.rule().incomeType();
        this.paymentMethod = occurrence.rule().paymentMethod();
        this.billingPeriod = occurrence.billingPeriod().toString();
        this.day = occurrence.rule().day();
        this.amount = occurrence.amount().amount();
        this.defaultAmount = occurrence.rule().defaultAmount().amount();
        this.amountOverridden = occurrence.amountOverridden();
        this.status = occurrence.status();
        this.effectiveDate = occurrence.effectiveDate();
        this.linkedEntryId = occurrence.linkedEntryId();
        this.linkCandidates = occurrence.linkCandidates() == null || occurrence.linkCandidates().isBlank() ? List.of()
                : Arrays.stream(occurrence.linkCandidates().split(",")).map(Long::valueOf).toList();
        this.version = occurrence.version();
    }
}

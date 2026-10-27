package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of occurrences (SOL-04).
 *
 * @since 1.0
 */
public interface OccurrenceRepository {

    /**
     * Finds an occurrence.
     *
     * @param id id
     * @return the occurrence, or empty
     */
    Optional<Occurrence> findById(Long id);

    /**
     * Lists the occurrences of a period and kind.
     *
     * @param billingPeriod period
     * @param kind          income or fixed expense
     * @return occurrences
     */
    List<Occurrence> findByBillingPeriodAndRuleKind(BillingPeriod billingPeriod, RuleKind kind);

    /**
     * Lists the occurrences of a rule.
     *
     * @param ruleId rule
     * @return occurrences
     */
    List<Occurrence> findByRuleId(long ruleId);

    /**
     * Lists the card-paid fixed expenses waiting for the user's choice of entry (RN-30).
     *
     * @return occurrences with candidates
     */
    List<Occurrence> findByLinkCandidatesIsNotNullOrderByBillingPeriod();

    /**
     * Stores an occurrence and writes it at once.
     *
     * @param occurrence occurrence
     * @return the stored occurrence
     */
    Occurrence saveAndFlush(Occurrence occurrence);
}

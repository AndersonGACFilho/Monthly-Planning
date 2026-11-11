package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.recurrence.domain.OccurrenceStatus;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ends an income or fixed expense with received or paid months from a chosen period: earlier months stay as they
 * are, later months still expected are removed (IMP-63).
 *
 * @since 1.0
 */
@Service
@Transactional
public class EndRecurrenceUseCase {

    private final RecurrenceRuleRepository rules;
    private final OccurrenceRepository occurrences;

    /**
     * Creates the use case.
     *
     * @param rules       rule storage
     * @param occurrences occurrence storage
     */
    public EndRecurrenceUseCase(RecurrenceRuleRepository rules, OccurrenceRepository occurrences) {
        this.rules = rules;
        this.occurrences = occurrences;
    }

    /**
     * Ends a rule from a period.
     *
     * @param id   rule
     * @param from first period without the rule
     * @return the ended rule
     * @throws BusinessRuleException with code {@code rule-received-after-end} if a month from {@code from} on was
     *         received or paid
     */
    public RecurrenceRule end(long id, BillingPeriod from) {
        if (occurrences.existsByRuleIdAndStatusAndBillingPeriodGreaterThanEqual(id, OccurrenceStatus.DONE, from)) {
            throw new BusinessRuleException("rule-received-after-end", id, from);
        }
        occurrences.deleteExpectedFrom(id, from);
        RecurrenceRule rule = rules.findById(id).orElseThrow(() -> new NotFoundException("recurrence-rule", id));
        rule.endBefore(from);
        return rules.saveAndFlush(rule);
    }
}

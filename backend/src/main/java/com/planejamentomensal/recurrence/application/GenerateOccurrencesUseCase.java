package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates the incomes and fixed expenses of a billing period from their rules, once (RF-22, RF-31).
 * Incomes and fixed expenses use this same use case (SPEC section 13, DRY).
 *
 * @since 1.0
 */
@Service
@Transactional
public class GenerateOccurrencesUseCase {

    private final RecurrenceRuleRepository rules;
    private final OccurrenceRepository occurrences;

    /**
     * Creates the use case.
     *
     * @param rules       rule storage
     * @param occurrences occurrence storage
     */
    public GenerateOccurrencesUseCase(RecurrenceRuleRepository rules, OccurrenceRepository occurrences) {
        this.rules = rules;
        this.occurrences = occurrences;
    }

    /**
     * Returns the occurrences of a period, creating the missing ones; expected occurrences follow a changed
     * default amount unless the period was changed by hand.
     *
     * @param period billing period
     * @param kind   income or fixed expense
     * @return occurrences of the period, by rule description
     */
    public List<Occurrence> generate(BillingPeriod period, RuleKind kind) {
        Map<Long, Occurrence> existing = occurrences.findByBillingPeriodAndRuleKind(period, kind).stream()
                .collect(Collectors.toMap(o -> o.rule().id(), Function.identity()));
        for (RecurrenceRule rule : rules.findByKind(kind)) {
            Occurrence occurrence = existing.get(rule.id());
            if (occurrence == null && rule.appliesTo(period)) {
                existing.put(rule.id(), occurrences.saveAndFlush(new Occurrence(rule, period)));
            } else if (occurrence != null) {
                occurrence.followDefault(rule.defaultAmount());
            }
        }
        return existing.values().stream()
                .sorted(Comparator.comparing((Occurrence o) -> o.rule().description(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

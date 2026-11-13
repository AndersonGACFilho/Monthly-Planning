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
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed expenses of a future period without generating its months: the months already generated as they
 * are, plus the rules that apply to the period with their default amount (IMP-64).
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class RecurrenceForecast {

    private final RecurrenceRuleRepository rules;
    private final OccurrenceRepository occurrences;

    /**
     * Creates the forecast.
     *
     * @param rules       rule storage
     * @param occurrences occurrence storage
     */
    public RecurrenceForecast(RecurrenceRuleRepository rules, OccurrenceRepository occurrences) {
        this.rules = rules;
        this.occurrences = occurrences;
    }

    /**
     * Forecasts the fixed expenses of a period.
     *
     * @param period billing period
     * @return the fixed expenses as they would be generated, by description
     */
    public List<OccurrenceView> fixedExpenses(BillingPeriod period) {
        Map<Long, Occurrence> stored = occurrences.findByBillingPeriodAndRuleKind(period, RuleKind.FIXED_EXPENSE).stream()
                .collect(Collectors.toMap(o -> o.rule().id(), Function.identity()));
        Stream<OccurrenceView> projected = rules.findByKind(RuleKind.FIXED_EXPENSE).stream()
                .filter(rule -> !stored.containsKey(rule.id()) && rule.appliesTo(period))
                .map(rule -> OccurrenceView.projected(rule, period));
        return Stream.concat(stored.values().stream().map(OccurrenceView::of), projected)
                .sorted(Comparator.comparing(OccurrenceView::description, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

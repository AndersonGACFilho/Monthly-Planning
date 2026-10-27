package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.category.application.CategoryReferences;
import org.springframework.stereotype.Component;

/**
 * Categories used by fixed expense rules cannot be deleted.
 *
 * @since 1.0
 */
@Component
public class RecurrenceReferences implements CategoryReferences {

    private final RecurrenceRuleRepository rules;

    /**
     * Creates the check.
     *
     * @param rules rule storage
     */
    public RecurrenceReferences(RecurrenceRuleRepository rules) {
        this.rules = rules;
    }

    /**
     * Tells whether a rule uses the category.
     *
     * @param categoryId category
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long categoryId) {
        return rules.existsByCategoryId(categoryId);
    }
}

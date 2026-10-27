package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import com.planejamentomensal.recurrence.domain.RuleKind;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of recurrence rules (SOL-04).
 *
 * @since 1.0
 */
public interface RecurrenceRuleRepository {

    /**
     * Finds a rule.
     *
     * @param id id
     * @return the rule, or empty
     */
    Optional<RecurrenceRule> findById(Long id);

    /**
     * Lists the rules of a kind.
     *
     * @param kind income or fixed expense
     * @return rules
     */
    List<RecurrenceRule> findByKind(RuleKind kind);

    /**
     * Lists every rule.
     *
     * @return rules
     */
    List<RecurrenceRule> findAll();

    /**
     * Tells whether a member has rules.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    boolean existsByMemberId(long memberId);

    /**
     * Tells whether a category has rules.
     *
     * @param categoryId category
     * @return {@code true} if referenced
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Stores a rule and writes it at once.
     *
     * @param rule rule
     * @return the stored rule
     */
    RecurrenceRule saveAndFlush(RecurrenceRule rule);

    /**
     * Removes a rule.
     *
     * @param rule rule
     */
    void delete(RecurrenceRule rule);
}

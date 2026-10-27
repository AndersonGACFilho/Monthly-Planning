package com.planejamentomensal.recurrence.application;

import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.stereotype.Component;

/**
 * Members with incomes or fixed expenses cannot be deleted (RF-01).
 *
 * @since 1.0
 */
@Component
public class RecurrenceMemberReferences implements MemberReferences {

    private final RecurrenceRuleRepository rules;

    /**
     * Creates the check.
     *
     * @param rules rule storage
     */
    public RecurrenceMemberReferences(RecurrenceRuleRepository rules) {
        this.rules = rules;
    }

    /**
     * Tells whether a rule uses the member.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long memberId) {
        return rules.existsByMemberId(memberId);
    }
}

package com.planejamentomensal.recurrence.infra;

import com.planejamentomensal.recurrence.application.OccurrenceRepository;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.shared.domain.BillingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA implementation of {@link OccurrenceRepository}.
 *
 * @since 1.0
 */
public interface JpaOccurrenceRepository extends JpaRepository<Occurrence, Long>, OccurrenceRepository {

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Occurrence o where o.rule.id = :ruleId")
    void deleteByRuleId(long ruleId);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Occurrence o where o.rule.id = :ruleId and o.billingPeriod >= :from
            and o.status = com.planejamentomensal.recurrence.domain.OccurrenceStatus.EXPECTED""")
    void deleteExpectedFrom(long ruleId, BillingPeriod from);
}

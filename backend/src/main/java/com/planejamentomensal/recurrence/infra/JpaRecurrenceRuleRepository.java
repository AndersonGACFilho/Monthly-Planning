package com.planejamentomensal.recurrence.infra;

import com.planejamentomensal.recurrence.application.RecurrenceRuleRepository;
import com.planejamentomensal.recurrence.domain.RecurrenceRule;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link RecurrenceRuleRepository}.
 *
 * @since 1.0
 */
public interface JpaRecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long>, RecurrenceRuleRepository {
}

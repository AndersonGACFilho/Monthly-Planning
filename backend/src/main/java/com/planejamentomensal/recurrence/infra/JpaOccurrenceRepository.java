package com.planejamentomensal.recurrence.infra;

import com.planejamentomensal.recurrence.application.OccurrenceRepository;
import com.planejamentomensal.recurrence.domain.Occurrence;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link OccurrenceRepository}.
 *
 * @since 1.0
 */
public interface JpaOccurrenceRepository extends JpaRepository<Occurrence, Long>, OccurrenceRepository {
}

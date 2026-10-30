package com.planejamentomensal.carryover.infra;

import com.planejamentomensal.carryover.application.CarryOverRepository;
import com.planejamentomensal.carryover.domain.CarryOver;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link CarryOverRepository}.
 *
 * @since 1.0
 */
public interface JpaCarryOverRepository extends JpaRepository<CarryOver, Long>, CarryOverRepository {
}

package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.StatementDeliveryRepository;
import com.planejamentomensal.statement.domain.StatementDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link StatementDeliveryRepository}.
 *
 * @since 1.0
 */
public interface JpaStatementDeliveryRepository extends JpaRepository<StatementDelivery, Long>, StatementDeliveryRepository {
}

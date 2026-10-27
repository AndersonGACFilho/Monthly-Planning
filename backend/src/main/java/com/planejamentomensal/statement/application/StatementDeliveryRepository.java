package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.StatementDelivery;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of {@link StatementDelivery} (SOL-04).
 *
 * @since 1.0
 */
public interface StatementDeliveryRepository {

    /**
     * Finds one by id.
     *
     * @param id id
     * @return the record, or empty
     */
    Optional<StatementDelivery> findById(Long id);

    /**
     * Lists the deliveries, newest first.
     *
     * @return deliveries
     */
    List<StatementDelivery> findAllByOrderByCreatedAtDescIdDesc();

    /**
     * Tells whether a member has deliveries.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    boolean existsByMemberId(long memberId);

    /**
     * Stores a record and writes it at once.
     *
     * @param value record
     * @return the stored record
     */
    StatementDelivery saveAndFlush(StatementDelivery value);
}

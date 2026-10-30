package com.planejamentomensal.carryover.application;

import com.planejamentomensal.carryover.domain.CarryOver;
import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of carry-overs (SOL-04).
 *
 * @since 1.0
 */
public interface CarryOverRepository {

    /**
     * Finds the carry-over of a member out of a period.
     *
     * @param memberId   member
     * @param fromPeriod source period
     * @return the carry-over, or empty
     */
    Optional<CarryOver> findByMemberIdAndFromPeriod(long memberId, BillingPeriod fromPeriod);

    /**
     * Lists the carry-overs out of a period.
     *
     * @param fromPeriod source period
     * @return carry-overs
     */
    List<CarryOver> findByFromPeriod(BillingPeriod fromPeriod);

    /**
     * Lists the carry-overs into a period.
     *
     * @param toPeriod target period
     * @return carry-overs
     */
    List<CarryOver> findByToPeriod(BillingPeriod toPeriod);

    /**
     * Tells whether a member has carry-overs.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    boolean existsByMemberId(long memberId);

    /**
     * Stores a carry-over and writes it at once.
     *
     * @param carryOver carry-over
     * @return the stored carry-over
     */
    CarryOver saveAndFlush(CarryOver carryOver);

    /**
     * Removes a carry-over.
     *
     * @param carryOver carry-over
     */
    void delete(CarryOver carryOver);
}

package com.planejamentomensal.carryover.application;

import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.stereotype.Component;

/**
 * Members with carry-overs cannot be deleted (RF-01).
 *
 * @since 1.0
 */
@Component
public class CarryOverMemberReferences implements MemberReferences {

    private final CarryOverRepository carryOvers;

    /**
     * Creates the check.
     *
     * @param carryOvers carry-over storage
     */
    public CarryOverMemberReferences(CarryOverRepository carryOvers) {
        this.carryOvers = carryOvers;
    }

    /**
     * Tells whether a carry-over uses the member.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long memberId) {
        return carryOvers.existsByMemberId(memberId);
    }
}

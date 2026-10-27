package com.planejamentomensal.statement.application;

import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.stereotype.Component;

/**
 * Members with sent statements cannot be deleted; the history refers to them (RF-01, RF-92).
 *
 * @since 1.0
 */
@Component
public class StatementMemberReferences implements MemberReferences {

    private final StatementDeliveryRepository deliveries;

    /**
     * Creates the check.
     *
     * @param deliveries history storage
     */
    public StatementMemberReferences(StatementDeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

    /**
     * Tells whether a delivery refers to the member.
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long memberId) {
        return deliveries.existsByMemberId(memberId);
    }
}

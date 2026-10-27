package com.planejamentomensal.entry.application;

import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.stereotype.Component;

/**
 * A member with a share in some purchase cannot be deleted (RF-01).
 *
 * @since 1.0
 */
@Component
public class PurchaseMemberReferences implements MemberReferences {

    private final PurchaseRepository purchases;
    private final CardBillRepository bills;

    /**
     * Creates the check.
     *
     * @param purchases purchase storage
     * @param bills     bill storage
     */
    public PurchaseMemberReferences(PurchaseRepository purchases, CardBillRepository bills) {
        this.purchases = purchases;
        this.bills = bills;
    }

    /**
     * Tells whether purchases use the member.
     *
     * @param memberId member id
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long memberId) {
        return purchases.existsBySplitsMemberId(memberId);
    }
}

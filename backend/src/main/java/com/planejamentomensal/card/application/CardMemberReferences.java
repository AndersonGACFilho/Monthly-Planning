package com.planejamentomensal.card.application;

import com.planejamentomensal.member.application.MemberReferences;
import org.springframework.stereotype.Component;

/**
 * A member that is the default member of a card cannot be deleted (RF-01, RF-02).
 *
 * @since 1.0
 */
@Component
public class CardMemberReferences implements MemberReferences {

    private final CardRepository cards;

    /**
     * Creates the check.
     *
     * @param cards card storage
     */
    public CardMemberReferences(CardRepository cards) {
        this.cards = cards;
    }

    /**
     * Tells whether a card uses the member as default.
     *
     * @param memberId member id
     * @return {@code true} if some card points to it
     */
    @Override
    public boolean isReferenced(long memberId) {
        return cards.countByDefaultMemberId(memberId) > 0;
    }
}

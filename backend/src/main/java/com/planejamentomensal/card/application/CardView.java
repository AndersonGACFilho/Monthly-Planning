package com.planejamentomensal.card.application;

import com.planejamentomensal.card.domain.Card;
import java.util.List;

/**
 * Read model of a card for other modules (SOL-06).
 *
 * @param id              id
 * @param nickname        nickname
 * @param lastDigits      finals
 * @param holderName      holder name as printed on the bill
 * @param primaryCardId   primary card of an additional card, or {@code null}
 * @param billingCardId   card whose bill holds this card's entries: the primary card, or itself (RF-05)
 * @param defaultMemberId default member (RF-02)
 * @param active          active flag
 * @since 1.0
 */
public record CardView(long id, String nickname, List<String> lastDigits, String holderName, Long primaryCardId,
        long billingCardId, long defaultMemberId, boolean active) {

    /**
     * Builds the view of a card.
     *
     * @param card card
     * @return its view
     */
    public static CardView of(Card card) {
        return new CardView(card.id(), card.nickname(), card.lastDigits(), card.holderName(), card.primaryCardId(),
                card.isAdditional() ? card.primaryCardId() : card.id(), card.defaultMemberId(), card.active());
    }
}

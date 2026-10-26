package com.planejamentomensal.card.domain;

import java.util.List;

/**
 * Editable data of a {@link Card}.
 *
 * @param nickname        nickname such as "Itaú Platinum"
 * @param bank            issuer
 * @param lastDigits      finals of the card numbers (physical, virtual, replacement), 4 digits each
 * @param holderName      cardholder name as printed on the bill
 * @param primaryCardId   primary card for an additional card, or {@code null}
 * @param defaultMemberId member used when an entry names none (RF-02)
 * @since 1.0
 */
public record CardDetails(String nickname, Bank bank, List<String> lastDigits, String holderName, Long primaryCardId,
        long defaultMemberId) {
}

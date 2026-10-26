package com.planejamentomensal.card.api;

import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.CardDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Data to create or replace a card (RF-02, RF-03, RF-05).
 *
 * @param nickname        nickname
 * @param bank            issuer
 * @param lastDigits      finals, 4 digits each
 * @param holderName      holder name as on the bill
 * @param primaryCardId   primary card for an additional card, optional
 * @param defaultMemberId default member
 * @param closingDay      closing day of the first cycle (used on creation)
 * @param dueDay          due day of the first cycle (used on creation)
 * @since 1.0
 */
public record CardRequest(
        @NotBlank @Size(max = 60) String nickname,
        @NotNull Bank bank,
        @NotEmpty List<@Pattern(regexp = "\\d{4}") String> lastDigits,
        @NotBlank @Size(max = 80) String holderName,
        Long primaryCardId,
        @NotNull Long defaultMemberId,
        @Min(1) @Max(31) int closingDay,
        @Min(1) @Max(31) int dueDay) {

    /**
     * Converts to domain data.
     *
     * @return card details
     */
    public CardDetails toDetails() {
        return new CardDetails(nickname.strip(), bank, lastDigits, holderName.strip().toUpperCase(), primaryCardId,
                defaultMemberId);
    }
}

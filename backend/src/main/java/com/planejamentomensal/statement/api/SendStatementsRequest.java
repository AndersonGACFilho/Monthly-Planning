package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.SendRequest;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.StatementOptions;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Batch delivery of statements (RF-91).
 *
 * @param billingPeriod  period, {@code yyyy-MM}
 * @param memberIds      members
 * @param channel        channel
 * @param showCategories show categories
 * @param useAlias       use aliases
 * @param includeFuture  include future installments
 * @param language       language for all, optional (I18N-09)
 * @param templateId     template, optional
 * @param note           free note, optional
 * @param sendAnyway     send even with entries pending review (RF-79b)
 * @since 1.0
 */
public record SendStatementsRequest(
        @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}$") String billingPeriod,
        @NotEmpty List<Long> memberIds,
        @NotNull DeliveryChannelType channel,
        Boolean showCategories,
        Boolean useAlias,
        Boolean includeFuture,
        @Pattern(regexp = "^(pt-BR|en-US)$") String language,
        Long templateId,
        @Size(max = 1000) String note,
        boolean sendAnyway) {

    /**
     * Converts to the use case request.
     *
     * @return send request
     */
    public SendRequest toSendRequest() {
        return new SendRequest(BillingPeriod.parse(billingPeriod), memberIds, channel,
                new StatementOptions(!Boolean.FALSE.equals(showCategories), !Boolean.FALSE.equals(useAlias),
                        !Boolean.FALSE.equals(includeFuture)),
                language, templateId, note, sendAnyway);
    }
}

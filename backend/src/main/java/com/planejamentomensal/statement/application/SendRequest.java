package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.util.List;

/**
 * A batch delivery (RF-91).
 *
 * @param period     billing period
 * @param memberIds  members, one statement each (RF-81)
 * @param channel    channel
 * @param options    export options
 * @param language   language for all of them, or {@code null} for each member's own (I18N-09)
 * @param templateId template, or {@code null}
 * @param note       free note, or {@code null}
 * @param sendAnyway {@code true} to send even with entries pending review (RF-79b)
 * @since 1.0
 */
public record SendRequest(BillingPeriod period, List<Long> memberIds, DeliveryChannelType channel,
        StatementOptions options, String language, Long templateId, String note, boolean sendAnyway) {
}

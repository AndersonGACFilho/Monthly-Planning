package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.StatementCustomization;
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
 * @param sendAnyway    {@code true} to send even with entries pending review (RF-79b)
 * @param customization header, footer, blocks and columns of this delivery (IMP-60)
 * @since 1.0
 */
public record SendRequest(BillingPeriod period, List<Long> memberIds, DeliveryChannelType channel,
        StatementOptions options, String language, Long templateId, String note, boolean sendAnyway,
        StatementCustomization customization) {

    /**
     * Creates a request that keeps the template as it is.
     *
     * @param period     billing period
     * @param memberIds  members
     * @param channel    channel
     * @param options    export options
     * @param language   language for all of them, or {@code null}
     * @param templateId template, or {@code null}
     * @param note       free note, or {@code null}
     * @param sendAnyway {@code true} to send even with entries pending review
     */
    public SendRequest(BillingPeriod period, List<Long> memberIds, DeliveryChannelType channel, StatementOptions options,
            String language, Long templateId, String note, boolean sendAnyway) {
        this(period, memberIds, channel, options, language, templateId, note, sendAnyway, StatementCustomization.NONE);
    }
}

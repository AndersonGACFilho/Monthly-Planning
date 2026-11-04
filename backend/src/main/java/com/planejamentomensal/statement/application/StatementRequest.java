package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.StatementCustomization;
import com.planejamentomensal.statement.domain.StatementOptions;

/**
 * What to put in one statement.
 *
 * @param memberId   member
 * @param period     billing period
 * @param options    export options (RF-83)
 * @param language   language of this export, or {@code null} for the member's preferred one (I18N-08, I18N-09)
 * @param templateId template, or {@code null} for the member's or the default (TPL-04)
 * @param note          free note, or {@code null} for the template's message (RF-82)
 * @param customization header, footer, blocks and columns of this export (IMP-60)
 * @since 1.0
 */
public record StatementRequest(long memberId, BillingPeriod period, StatementOptions options, String language,
        Long templateId, String note, StatementCustomization customization) {

    /**
     * Creates a request that keeps the template as it is.
     *
     * @param memberId   member
     * @param period     billing period
     * @param options    export options (RF-83)
     * @param language   language of this export, or {@code null} for the member's preferred one
     * @param templateId template, or {@code null} for the member's or the default
     * @param note       free note, or {@code null} for the template's message
     */
    public StatementRequest(long memberId, BillingPeriod period, StatementOptions options, String language,
            Long templateId, String note) {
        this(memberId, period, options, language, templateId, note, StatementCustomization.NONE);
    }
}

package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.domain.StatementOptions;

/**
 * What to put in one statement.
 *
 * @param memberId   member
 * @param period     billing period
 * @param options    export options (RF-83)
 * @param language   language of this export, or {@code null} for the member's preferred one (I18N-08, I18N-09)
 * @param templateId template, or {@code null} for the member's or the default (TPL-04)
 * @param note       free note, or {@code null} for the template's message (RF-82)
 * @since 1.0
 */
public record StatementRequest(long memberId, BillingPeriod period, StatementOptions options, String language,
        Long templateId, String note) {
}

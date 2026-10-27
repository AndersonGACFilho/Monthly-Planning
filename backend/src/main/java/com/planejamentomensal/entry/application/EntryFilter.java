package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;

/**
 * Filters of the entries list (RF-40, API-06); {@code null} fields do not filter.
 *
 * @param billingPeriod period
 * @param cardId        primary card
 * @param memberId      member with a share
 * @param categoryId    category; the "not classified" id also matches entries without category
 * @param type          {@code -}, {@code n/N} or {@code *}
 * @param text          text in the original description or alias (RF-51)
 * @param status        {@code PROJECTED}, {@code CONFIRMED} or {@code UNCONFIRMED}
 * @since 1.0
 */
public record EntryFilter(BillingPeriod billingPeriod, Long cardId, Long memberId, Long categoryId, String type,
        String text, String status) {
}

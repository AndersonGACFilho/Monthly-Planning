package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;

/**
 * One income of a member in the period (RF-22).
 *
 * @param memberId member
 * @param amount   amount
 * @since 1.0
 */
public record IncomeLine(long memberId, Money amount) {
}

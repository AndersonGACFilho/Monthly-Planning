package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.DeliveryStatus;

/**
 * Status of one member in a batch delivery (RF-91).
 *
 * @param memberId   member
 * @param deliveryId history record
 * @param status     result
 * @param detail     error message or saved file, or {@code null}
 * @param action     link the interface opens to finish, or {@code null}
 * @since 1.0
 */
public record DeliveryOutcome(long memberId, long deliveryId, DeliveryStatus status, String detail, String action) {
}

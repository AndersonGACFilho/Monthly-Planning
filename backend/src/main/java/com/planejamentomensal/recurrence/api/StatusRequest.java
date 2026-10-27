package com.planejamentomensal.recurrence.api;

import java.time.LocalDate;

/**
 * Received or paid flag of a period (RF-24, RF-32).
 *
 * @param done {@code true} for received or paid, {@code false} for expected or pending
 * @param date receipt or payment date; today when missing
 * @since 1.0
 */
public record StatusRequest(boolean done, LocalDate date) {
}

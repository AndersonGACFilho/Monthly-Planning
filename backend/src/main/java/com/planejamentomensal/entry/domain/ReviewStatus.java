package com.planejamentomensal.entry.domain;

/**
 * Review state of a purchase in the validation queue (RF-74).
 *
 * @since 1.0
 */
public enum ReviewStatus {
    /** Waiting for the user to check alias, member, split and category. */
    PENDING_REVIEW,
    /** Validated by the user, or entered by hand (RF-79c). */
    REVIEWED
}

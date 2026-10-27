package com.planejamentomensal.recurrence.api;

import jakarta.validation.constraints.NotNull;

/**
 * Card entry chosen for a fixed expense paid by card (RN-30).
 *
 * @param entryId chosen entry
 * @since 1.0
 */
public record LinkRequest(@NotNull Long entryId) {
}

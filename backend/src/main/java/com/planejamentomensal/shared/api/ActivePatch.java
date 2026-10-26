package com.planejamentomensal.shared.api;

import jakarta.validation.constraints.NotNull;

/**
 * Partial change of the active flag of a member, card or category (RF-01).
 *
 * @param active new state
 * @since 1.0
 */
public record ActivePatch(@NotNull Boolean active) {
}

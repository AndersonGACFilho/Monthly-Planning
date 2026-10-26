package com.planejamentomensal.member.api;

import jakarta.validation.constraints.NotNull;

/**
 * Partial change of the active flag (RF-01).
 *
 * @param active new state
 * @since 1.0
 */
public record ActivePatch(@NotNull Boolean active) {
}

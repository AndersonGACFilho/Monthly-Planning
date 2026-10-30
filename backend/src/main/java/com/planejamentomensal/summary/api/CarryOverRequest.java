package com.planejamentomensal.summary.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Third-party members whose unpaid amount is carried into the next period (RF-28).
 *
 * @param memberIds members
 * @since 1.0
 */
public record CarryOverRequest(@NotEmpty List<@NotNull Long> memberIds) {
}

package com.planejamentomensal.imports.api;

import com.planejamentomensal.imports.domain.LineDecision;
import jakarta.validation.constraints.Size;

/**
 * Change of a preview line (RF-69, RF-70); {@code null} fields keep their value.
 *
 * @param decision   decision
 * @param memberId   member
 * @param categoryId category
 * @param aliasName  alias; blank removes it
 * @since 1.0
 */
public record LineEditRequest(LineDecision decision, Long memberId, Long categoryId, @Size(max = 80) String aliasName) {
}

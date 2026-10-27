package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.domain.TemplateSettings;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Live preview of unsaved template settings (TPL-03).
 *
 * @param settings      settings being edited
 * @param billingPeriod period, {@code yyyy-MM}
 * @param memberId      member with real data, or {@code null} for sample data
 * @param language      language of the preview
 * @since 1.0
 */
public record TemplatePreviewRequest(@NotNull TemplateSettings settings,
        @NotNull @Pattern(regexp = "^\\d{4}-\\d{2}$") String billingPeriod, Long memberId,
        @NotNull @Pattern(regexp = "^(pt-BR|en-US)$") String language) {
}

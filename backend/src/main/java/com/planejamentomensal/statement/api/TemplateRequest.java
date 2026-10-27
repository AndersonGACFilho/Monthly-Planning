package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.domain.TemplateSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Name and settings of a template (TPL-02).
 *
 * @param name     name
 * @param settings settings
 * @since 1.0
 */
public record TemplateRequest(@NotBlank @Size(max = 80) String name, @NotNull TemplateSettings settings) {
}

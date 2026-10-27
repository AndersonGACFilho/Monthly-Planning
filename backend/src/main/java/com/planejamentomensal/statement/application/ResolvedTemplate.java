package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.TemplateSettings;

/**
 * Template used for one statement, with the version to record in the history (TPL-06).
 *
 * @param templateId template
 * @param version    version number
 * @param settings   settings of that version
 * @since 1.0
 */
public record ResolvedTemplate(long templateId, int version, TemplateSettings settings) {
}

package com.planejamentomensal.statement.api;

import java.time.Instant;

/**
 * One frozen version of a template (TPL-06).
 *
 * @param number    version number
 * @param createdAt time
 * @since 1.0
 */
public record TemplateVersionModel(int number, Instant createdAt) {
}

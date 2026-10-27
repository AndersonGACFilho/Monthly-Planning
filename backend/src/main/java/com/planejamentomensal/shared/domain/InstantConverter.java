package com.planejamentomensal.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Instant;

/**
 * Stores instants as ISO-8601 UTC text in SQLite, read back exactly.
 *
 * @since 1.0
 */
@Converter(autoApply = true)
public class InstantConverter implements AttributeConverter<Instant, String> {

    /**
     * Creates the converter.
     */
    public InstantConverter() {
        // stateless
    }

    /**
     * Converts to text.
     *
     * @param instant instant, possibly {@code null}
     * @return such as {@code 2026-10-27T13:31:43.873Z}, or {@code null}
     */
    @Override
    public String convertToDatabaseColumn(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /**
     * Converts from text.
     *
     * @param text ISO-8601 instant, possibly {@code null}
     * @return the instant, or {@code null}
     */
    @Override
    public Instant convertToEntityAttribute(String text) {
        return text == null ? null : Instant.parse(text);
    }
}

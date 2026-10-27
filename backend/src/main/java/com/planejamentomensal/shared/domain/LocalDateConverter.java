package com.planejamentomensal.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

/**
 * Stores dates as ISO text ({@code 2026-10-14}) in SQLite, which sorts and compares correctly and reads
 * back exactly; the driver's default (epoch milliseconds in a TEXT column) does not survive a round trip.
 *
 * @since 1.0
 */
@Converter(autoApply = true)
public class LocalDateConverter implements AttributeConverter<LocalDate, String> {

    /**
     * Creates the converter.
     */
    public LocalDateConverter() {
        // stateless
    }

    /**
     * Converts to text.
     *
     * @param date date, possibly {@code null}
     * @return {@code yyyy-MM-dd}, or {@code null}
     */
    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /**
     * Converts from text.
     *
     * @param text {@code yyyy-MM-dd}, possibly {@code null}
     * @return the date, or {@code null}
     */
    @Override
    public LocalDate convertToEntityAttribute(String text) {
        return text == null ? null : LocalDate.parse(text);
    }
}

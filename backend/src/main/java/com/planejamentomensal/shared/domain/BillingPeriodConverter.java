package com.planejamentomensal.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores a {@link BillingPeriod} as {@code yyyy-MM} text, which also sorts chronologically.
 *
 * @since 1.0
 */
@Converter(autoApply = true)
public class BillingPeriodConverter implements AttributeConverter<BillingPeriod, String> {

    /**
     * Creates the converter.
     */
    public BillingPeriodConverter() {
        // stateless
    }

    /**
     * Converts to text.
     *
     * @param period period, possibly {@code null}
     * @return {@code yyyy-MM}, or {@code null}
     */
    @Override
    public String convertToDatabaseColumn(BillingPeriod period) {
        return period == null ? null : period.toString();
    }

    /**
     * Converts from text.
     *
     * @param text {@code yyyy-MM}, possibly {@code null}
     * @return the period, or {@code null}
     */
    @Override
    public BillingPeriod convertToEntityAttribute(String text) {
        return text == null ? null : BillingPeriod.parse(text);
    }
}

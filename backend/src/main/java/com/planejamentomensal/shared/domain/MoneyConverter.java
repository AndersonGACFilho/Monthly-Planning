package com.planejamentomensal.shared.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores {@link Money} as a whole number of cents (RNF-01).
 *
 * @since 1.0
 */
@Converter(autoApply = true)
public class MoneyConverter implements AttributeConverter<Money, Long> {

    /**
     * Creates the converter.
     */
    public MoneyConverter() {
        // stateless
    }

    /**
     * Converts to cents.
     *
     * @param money amount, possibly {@code null}
     * @return cents, or {@code null}
     */
    @Override
    public Long convertToDatabaseColumn(Money money) {
        return money == null ? null : money.cents();
    }

    /**
     * Converts from cents.
     *
     * @param cents cents, possibly {@code null}
     * @return amount, or {@code null}
     */
    @Override
    public Money convertToEntityAttribute(Long cents) {
        return cents == null ? null : Money.ofCents(cents);
    }
}

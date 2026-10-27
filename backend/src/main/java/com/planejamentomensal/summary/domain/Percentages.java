package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Percent of one amount over another, with one decimal place.
 *
 * @since 1.0
 */
public final class Percentages {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private Percentages() {
    }

    /**
     * Calculates {@code part ÷ whole × 100}.
     *
     * @param part  part
     * @param whole whole
     * @return the percentage, or {@code null} when the whole is zero
     */
    public static BigDecimal of(Money part, Money whole) {
        if (whole.isZero()) {
            return null;
        }
        return part.amount().multiply(HUNDRED).divide(whole.amount(), 1, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the change from {@code before} to {@code now} in percent.
     *
     * @param now    current amount
     * @param before previous amount
     * @return the variation, or {@code null} when there was nothing before
     */
    public static BigDecimal variation(Money now, Money before) {
        if (before.isZero()) {
            return null;
        }
        return now.minus(before).amount().multiply(HUNDRED).divide(before.abs().amount(), 1, RoundingMode.HALF_UP);
    }
}

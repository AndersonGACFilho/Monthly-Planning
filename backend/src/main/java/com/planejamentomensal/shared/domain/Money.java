package com.planejamentomensal.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Amount in Brazilian reais with exact cents.
 *
 * <p>Implements RNF-01: money is never a floating-point number. Amounts are kept as a {@link BigDecimal}
 * with scale 2 and stored as a whole number of cents. All arithmetic, rounding and allocation of money
 * lives here (SPEC section 13, DRY).</p>
 *
 * @param amount value with scale 2; negative for refunds and credits
 * @since 1.0
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    /** Zero reais. */
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    /**
     * Normalizes the amount to scale 2.
     *
     * @param amount value; must not be {@code null}; more than two decimals are rounded half-even
     * @throws NullPointerException if {@code amount} is {@code null}
     */
    public Money {
        amount = Objects.requireNonNull(amount, "amount").setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * Creates an amount from a decimal text such as {@code "1234.56"}.
     *
     * @param amount decimal text with a dot as separator
     * @return the amount
     * @throws NumberFormatException if the text is not a number
     */
    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    /**
     * Creates an amount from a decimal value.
     *
     * @param amount value
     * @return the amount
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * Creates an amount from a whole number of cents.
     *
     * @param cents cents, such as {@code 123456} for R$ 1.234,56
     * @return the amount
     */
    public static Money ofCents(long cents) {
        return new Money(BigDecimal.valueOf(cents, 2));
    }

    /**
     * Returns the amount as cents.
     *
     * @return whole number of cents
     */
    public long cents() {
        return amount.movePointRight(2).longValueExact();
    }

    /**
     * Adds another amount.
     *
     * @param other amount to add
     * @return the sum
     */
    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    /**
     * Subtracts another amount.
     *
     * @param other amount to subtract
     * @return the difference
     */
    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /**
     * Multiplies by a whole number, such as an installment amount by the installment count.
     *
     * @param factor multiplier
     * @return the product
     */
    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    /**
     * Applies a percentage, rounding half-even to cents.
     *
     * @param percentage percentage such as {@code 50} or {@code 33.33}
     * @return the share of this amount
     */
    public Money percent(BigDecimal percentage) {
        return new Money(amount.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN));
    }

    /**
     * Returns the opposite amount.
     *
     * @return {@code -this}
     */
    public Money negate() {
        return new Money(amount.negate());
    }

    /**
     * Returns the absolute amount.
     *
     * @return {@code |this|}
     */
    public Money abs() {
        return new Money(amount.abs());
    }

    /**
     * Tells whether the amount is below zero.
     *
     * @return {@code true} for refunds and credits
     */
    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /**
     * Tells whether the amount is zero.
     *
     * @return {@code true} if zero
     */
    public boolean isZero() {
        return amount.signum() == 0;
    }

    /**
     * Tells whether two amounts differ by at most a tolerance.
     *
     * @param other     amount to compare
     * @param tolerance largest accepted absolute difference, such as R$ 0,02 (RN-18)
     * @return {@code true} if {@code |this - other| <= tolerance}
     */
    public boolean isCloseTo(Money other, Money tolerance) {
        return minus(other).abs().compareTo(tolerance) <= 0;
    }

    /**
     * Splits this amount by weights without losing cents: the remainder goes, one cent at a time, to the
     * shares with the largest rounding loss (RF-26).
     *
     * @param weights non-negative weights, such as percentages or amounts; at least one must be positive
     * @return one share per weight, summing exactly to this amount
     * @throws IllegalArgumentException if the weights are empty, negative or all zero
     */
    public List<Money> allocate(List<BigDecimal> weights) {
        if (weights.isEmpty() || weights.stream().anyMatch(w -> w.signum() < 0)) {
            throw new IllegalArgumentException("Weights must be non-negative and not empty");
        }
        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            throw new IllegalArgumentException("At least one weight must be positive");
        }
        long cents = cents();
        long sign = Long.signum(cents);
        long absolute = Math.abs(cents);
        List<Long> shares = new ArrayList<>();
        List<BigDecimal> remainders = new ArrayList<>();
        long allocated = 0;
        for (BigDecimal weight : weights) {
            BigDecimal exact = BigDecimal.valueOf(absolute).multiply(weight).divide(total, 10, RoundingMode.DOWN);
            long floor = exact.setScale(0, RoundingMode.DOWN).longValue();
            shares.add(floor);
            remainders.add(exact.subtract(BigDecimal.valueOf(floor)));
            allocated += floor;
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < weights.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> remainders.get(b).compareTo(remainders.get(a)));
        for (int i = 0; allocated < absolute; i = (i + 1) % order.size()) {
            int index = order.get(i);
            shares.set(index, shares.get(index) + 1);
            allocated++;
        }
        List<Money> result = new ArrayList<>();
        for (long share : shares) {
            result.add(ofCents(sign * share));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Compares amounts by value.
     *
     * @param other amount to compare
     * @return negative, zero or positive
     */
    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    /**
     * Returns the plain decimal text, such as {@code "1234.56"}; formatting for people is done by the
     * interface (I18N-05).
     *
     * @return decimal text
     */
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}

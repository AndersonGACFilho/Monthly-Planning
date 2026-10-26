package com.planejamentomensal.shared.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Billing period (competência): the month and year of a card bill's due date.
 *
 * <p>Implements the glossary of SPEC section 2 and RN-02: a bill due on 10/11/2026 belongs to 2026-11.
 * Next/previous month arithmetic lives only here (SPEC section 13, DRY).</p>
 *
 * @param yearMonth month and year
 * @since 1.0
 */
public record BillingPeriod(YearMonth yearMonth) implements Comparable<BillingPeriod> {

    /**
     * Validates the period.
     *
     * @param yearMonth month and year; must not be {@code null}
     * @throws NullPointerException if {@code yearMonth} is {@code null}
     */
    public BillingPeriod {
        Objects.requireNonNull(yearMonth, "yearMonth");
    }

    /**
     * Creates a period.
     *
     * @param year  year, such as 2026
     * @param month month from 1 to 12
     * @return the period
     */
    public static BillingPeriod of(int year, int month) {
        return new BillingPeriod(YearMonth.of(year, month));
    }

    /**
     * Returns the period of a due date (RN-02).
     *
     * @param dueDate due date of a bill
     * @return the period of that month
     */
    public static BillingPeriod ofDueDate(LocalDate dueDate) {
        return new BillingPeriod(YearMonth.from(dueDate));
    }

    /**
     * Parses {@code "2026-11"}.
     *
     * @param text period as {@code yyyy-MM}
     * @return the period
     * @throws IllegalArgumentException if the text is not a valid period
     */
    public static BillingPeriod parse(String text) {
        try {
            return new BillingPeriod(YearMonth.parse(text));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid billing period: " + text, e);
        }
    }

    /**
     * Returns the period {@code months} months later (negative goes back).
     *
     * @param months months to add
     * @return the shifted period
     */
    public BillingPeriod plusMonths(long months) {
        return new BillingPeriod(yearMonth.plusMonths(months));
    }

    /**
     * Returns the next period.
     *
     * @return one month later, crossing the year when needed
     */
    public BillingPeriod next() {
        return plusMonths(1);
    }

    /**
     * Returns the previous period.
     *
     * @return one month earlier
     */
    public BillingPeriod previous() {
        return plusMonths(-1);
    }

    /**
     * Returns a day of this period, clamped to the month length (day 31 in February is the 28th or 29th).
     *
     * @param day day of month, 1 to 31
     * @return the date
     */
    public LocalDate atDay(int day) {
        return yearMonth.atDay(Math.min(Math.max(day, 1), yearMonth.lengthOfMonth()));
    }

    /**
     * Returns the number of months from this period to another.
     *
     * @param other later (positive) or earlier (negative) period
     * @return months between them
     */
    public long monthsUntil(BillingPeriod other) {
        return yearMonth.until(other.yearMonth, java.time.temporal.ChronoUnit.MONTHS);
    }

    /**
     * Returns the year.
     *
     * @return year
     */
    public int year() {
        return yearMonth.getYear();
    }

    /**
     * Returns the month.
     *
     * @return month from 1 to 12
     */
    public int month() {
        return yearMonth.getMonthValue();
    }

    /**
     * Orders periods chronologically.
     *
     * @param other period to compare
     * @return negative, zero or positive
     */
    @Override
    public int compareTo(BillingPeriod other) {
        return yearMonth.compareTo(other.yearMonth);
    }

    /**
     * Returns {@code yyyy-MM}, the form used in URLs and JSON.
     *
     * @return such as {@code "2026-11"}
     */
    @Override
    public String toString() {
        return yearMonth.toString();
    }
}

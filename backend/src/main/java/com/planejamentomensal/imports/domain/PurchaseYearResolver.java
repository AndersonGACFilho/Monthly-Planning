package com.planejamentomensal.imports.domain;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;

/**
 * Finds the year of a {@code dd/mm} date printed on a PDF bill (RF-64f).
 *
 * <p>For an installment {@code n/N} the year is the one that brings the date closest to
 * {@code closing - (n - 1) months} without passing the closing date; for a single purchase, the latest year
 * that keeps the date on or before the closing date. Example: {@code 14/10 … 12/12} on a bill closing on
 * 28/09/2026 is 14/10/2025.</p>
 *
 * @since 1.0
 */
public final class PurchaseYearResolver {

    private PurchaseYearResolver() {
    }

    /**
     * Resolves the full date.
     *
     * @param dayMonth          day and month as printed
     * @param closingDate       closing date of the bill
     * @param installmentNumber installment number, or {@code null} for a single purchase
     * @return the purchase date
     */
    public static LocalDate resolve(MonthDay dayMonth, LocalDate closingDate, Integer installmentNumber) {
        int monthsBack = installmentNumber == null ? 0 : installmentNumber - 1;
        LocalDate target = closingDate.minusMonths(monthsBack);
        LocalDate best = null;
        for (int year = closingDate.getYear() - 4; year <= closingDate.getYear(); year++) {
            if (!dayMonth.isValidYear(year)) {
                continue;
            }
            LocalDate candidate = dayMonth.atYear(year);
            if (candidate.isAfter(closingDate)) {
                continue;
            }
            if (best == null || distance(candidate, target) < distance(best, target)) {
                best = candidate;
            }
        }
        return best != null ? best : dayMonth.atYear(closingDate.getYear() - 1);
    }

    private static long distance(LocalDate a, LocalDate b) {
        return Math.abs(ChronoUnit.DAYS.between(a, b));
    }
}

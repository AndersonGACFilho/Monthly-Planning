package com.planejamentomensal.entry.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

/**
 * Billing period of a purchase's first installment (RN-01 to RN-03, RN-22).
 *
 * <p>Purchases up to the closing day go to the bill that closes that month; later ones go to the next bill
 * (RN-01). The period is the month of that bill's due date (RN-02). Closing dates read from files win over
 * the card's typical day (RN-22). On import, the period chosen by the user wins (RN-03); this policy is
 * used for manual purchases and projections.</p>
 *
 * @since 1.0
 */
public final class BillingPeriodPolicy {

    private BillingPeriodPolicy() {
    }

    /**
     * Calculates the billing period of the first installment.
     *
     * @param purchaseDate   date of the purchase at the merchant
     * @param closingDay     typical closing day of the card
     * @param dueDay         typical due day of the card
     * @param knownClosings  closing dates read from files, by billing period (RN-22); may be empty
     * @return the billing period of the first installment
     */
    public static BillingPeriod firstPeriod(LocalDate purchaseDate, int closingDay, int dueDay,
            Map<BillingPeriod, LocalDate> knownClosings) {
        LocalDate closing = closingOnOrAfter(purchaseDate, closingDay, knownClosings);
        return periodOfClosing(closing, dueDay, knownClosings);
    }

    /**
     * Returns the due date that follows a closing date: the first due day after it.
     *
     * @param closing closing date
     * @param dueDay  typical due day
     * @return due date
     */
    public static LocalDate dueAfter(LocalDate closing, int dueDay) {
        BillingPeriod sameMonth = BillingPeriod.ofDueDate(closing);
        LocalDate candidate = sameMonth.atDay(dueDay);
        return candidate.isAfter(closing) ? candidate : sameMonth.next().atDay(dueDay);
    }

    private static LocalDate closingOnOrAfter(LocalDate date, int closingDay, Map<BillingPeriod, LocalDate> known) {
        Map<YearMonth, LocalDate> byMonth = new HashMap<>();
        known.values().forEach(closing -> byMonth.put(YearMonth.from(closing), closing));
        BillingPeriod month = BillingPeriod.ofDueDate(date);
        for (int i = 0; i < 3; i++) {
            BillingPeriod candidateMonth = month.plusMonths(i);
            LocalDate closing = byMonth.getOrDefault(candidateMonth.yearMonth(), candidateMonth.atDay(closingDay));
            if (!date.isAfter(closing)) {
                return closing;
            }
        }
        return month.plusMonths(3).atDay(closingDay);
    }

    private static BillingPeriod periodOfClosing(LocalDate closing, int dueDay, Map<BillingPeriod, LocalDate> known) {
        return known.entrySet().stream()
                .filter(e -> e.getValue().equals(closing))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseGet(() -> BillingPeriod.ofDueDate(dueAfter(closing, dueDay)));
    }
}

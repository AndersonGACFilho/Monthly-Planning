package com.planejamentomensal.shared.domain;

import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Tolerant comparison of installments read from different formats (RN-16 to RN-19).
 *
 * <p>The same installment shows up in the PDF and in the XLSX with different text, a purchase date one day
 * apart and cents of difference, so two installments are the same when they have the same card, the same
 * installment {@code n/N}, purchase dates at most one day apart (RN-17), amounts at most R$ 0,02 apart
 * (RN-18) and descriptions of the same store by prefix (RN-16). This replaces the exact description of
 * RN-10 (RN-19).</p>
 *
 * @since 1.0
 */
public final class PurchaseMatcher {

    /** Largest accepted difference in purchase date, in days (RN-17). */
    public static final int DATE_TOLERANCE_DAYS = 1;
    /** Largest accepted difference in amount (RN-18). */
    public static final Money AMOUNT_TOLERANCE = Money.of("0.02");

    private PurchaseMatcher() {
    }

    /**
     * Tells whether two installments are the same (RN-19).
     *
     * @param a first installment
     * @param b second installment
     * @return {@code true} if card, installment, date ±1, amount ±0,02 and store match
     */
    public static boolean sameInstallment(PurchaseFacts a, PurchaseFacts b) {
        return sameIdentity(a, b) && closeDates(a, b) && a.amount().isCloseTo(b.amount(), AMOUNT_TOLERANCE);
    }

    /**
     * Tells whether two installments look like the same one but differ in amount or date (RN-13 step 3).
     *
     * @param a first installment
     * @param b second installment
     * @return {@code true} for the same card, store and installment with a different amount or date
     */
    public static boolean divergent(PurchaseFacts a, PurchaseFacts b) {
        return sameIdentity(a, b) && !sameInstallment(a, b);
    }

    private static boolean sameIdentity(PurchaseFacts a, PurchaseFacts b) {
        return a.cardId() == b.cardId()
                && Objects.equals(a.installmentNumber(), b.installmentNumber())
                && Objects.equals(a.installmentCount(), b.installmentCount())
                && sameStore(a.description(), b.description());
    }

    private static boolean closeDates(PurchaseFacts a, PurchaseFacts b) {
        return Math.abs(ChronoUnit.DAYS.between(a.purchaseDate(), b.purchaseDate())) <= DATE_TOLERANCE_DAYS;
    }

    private static boolean sameStore(String a, String b) {
        return DescriptionNormalizer.normalize(a).equals(DescriptionNormalizer.normalize(b))
                || DescriptionNormalizer.sameStore(a, b);
    }
}

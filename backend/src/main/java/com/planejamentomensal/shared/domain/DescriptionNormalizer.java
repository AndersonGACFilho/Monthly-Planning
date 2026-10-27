package com.planejamentomensal.shared.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes card descriptions and separates the installment suffix.
 *
 * <p>Implements RN-15 (no accents, upper case, only letters and digits: "Posto Agronomiaporto Alegrebra"
 * becomes {@code POSTOAGRONOMIAPORTOALEGREBRA}) and RF-65 (the {@code NN/NN} installment suffix of PDF
 * descriptions is removed before normalizing). Used by imports, entries and aliases (RF-46).</p>
 *
 * @since 1.0
 */
public final class DescriptionNormalizer {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");
    private static final Pattern INSTALLMENT_SUFFIX = Pattern.compile("^(.*?)\\s+(\\d{1,2})/(\\d{1,2})\\s*$");

    private DescriptionNormalizer() {
    }

    /**
     * Description split into text and installment.
     *
     * @param text  description without the installment suffix, trimmed
     * @param number installment number, or {@code null} when there was no suffix
     * @param count  installment count, or {@code null}
     */
    public record Split(String text, Integer number, Integer count) {
    }

    /**
     * Normalizes a description (RN-15).
     *
     * @param description original description
     * @return upper-case letters and digits only
     */
    public static String normalize(String description) {
        String ascii = MARKS.matcher(Normalizer.normalize(description, Normalizer.Form.NFD)).replaceAll("");
        return NOT_ALPHANUMERIC.matcher(ascii.toUpperCase(Locale.ROOT)).replaceAll("");
    }

    /**
     * Separates a trailing {@code NN/NN} installment, as in "MERCADOLIVRE*M 02/02" (RF-65).
     *
     * @param description original description
     * @return the text and the installment, if any
     */
    public static Split splitInstallment(String description) {
        Matcher matcher = INSTALLMENT_SUFFIX.matcher(description);
        if (matcher.matches()) {
            int number = Integer.parseInt(matcher.group(2));
            int count = Integer.parseInt(matcher.group(3));
            if (number >= 1 && count >= number) {
                return new Split(matcher.group(1).strip(), number, count);
            }
        }
        return new Split(description.strip(), null, null);
    }

    /**
     * Tells whether two descriptions are the same store: one normalized form is a prefix of the other and
     * the shorter has at least 8 characters (RN-16).
     *
     * @param a first description, normalized or not
     * @param b second description, normalized or not
     * @return {@code true} for the same store
     */
    public static boolean sameStore(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        String shorter = x.length() <= y.length() ? x : y;
        String longer = shorter == x ? y : x;
        return shorter.length() >= 8 && longer.startsWith(shorter);
    }

    /**
     * Returns the common store prefix of two descriptions, if they are the same store (RN-16, RN-24).
     *
     * @param a first description
     * @param b second description
     * @return the shorter normalized description, or empty
     */
    public static Optional<String> commonStore(String a, String b) {
        if (!sameStore(a, b)) {
            return Optional.empty();
        }
        String x = normalize(a);
        String y = normalize(b);
        return Optional.of(x.length() <= y.length() ? x : y);
    }
}

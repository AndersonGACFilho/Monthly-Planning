package com.planejamentomensal.imports.infra.itau;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text conventions of Itaú files: Brazilian amounts, dates and month names.
 *
 * @since 1.0
 */
final class ItauText {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern MONEY = Pattern.compile("^-?R?\\$?\\s*-?[\\d.]*\\d,\\d{2}$");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("janeiro", 1), Map.entry("fevereiro", 2), Map.entry("marco", 3), Map.entry("abril", 4),
            Map.entry("maio", 5), Map.entry("junho", 6), Map.entry("julho", 7), Map.entry("agosto", 8),
            Map.entry("setembro", 9), Map.entry("outubro", 10), Map.entry("novembro", 11), Map.entry("dezembro", 12));

    private ItauText() {
    }

    /**
     * Parses "1.234,56", "-5.385,41" or "R$ 6.514,13".
     *
     * @param text amount as printed
     * @return the amount
     * @throws NumberFormatException if it is not an amount
     */
    static Money money(String text) {
        String cleaned = text.replace("R$", "").replace(" ", "").strip();
        if (!MONEY.matcher(cleaned).matches()) {
            throw new NumberFormatException("Not an amount: " + text);
        }
        boolean negative = cleaned.startsWith("-");
        String digits = cleaned.replace("-", "").replace("$", "").strip().replace(".", "").replace(",", ".");
        BigDecimal value = new BigDecimal(digits);
        return Money.of(negative ? value.negate() : value);
    }

    /**
     * Parses dd/MM/yyyy.
     *
     * @param text date as printed
     * @return the date
     */
    static LocalDate date(String text) {
        return LocalDate.parse(text.strip(), DATE);
    }

    /**
     * Finds the first dd/MM/yyyy after a label, such as "Vencimento: 09/10/2026".
     *
     * @param text  page text
     * @param label label before the date
     * @return the date, or empty
     */
    static Optional<LocalDate> dateAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*:?\\s*(\\d{2}/\\d{2}/\\d{4})").matcher(text);
        return matcher.find() ? Optional.of(date(matcher.group(1))) : Optional.empty();
    }

    /**
     * Finds the first amount after a label, such as "Total desta fatura 6.514,13".
     *
     * @param text  page text
     * @param label label before the amount
     * @return the amount, or empty
     */
    static Optional<Money> moneyAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*(?:R\\$)?\\s*(-?[\\d.]*\\d,\\d{2})").matcher(text);
        return matcher.find() ? Optional.of(money(matcher.group(1))) : Optional.empty();
    }

    /**
     * Parses "Novembro/2026" into a billing period.
     *
     * @param monthName Portuguese month name, any case, with or without accents
     * @param year      year
     * @return the period, or empty for an unknown month
     */
    static Optional<BillingPeriod> period(String monthName, int year) {
        String key = Normalizer.normalize(monthName, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).strip();
        return Optional.ofNullable(MONTHS.get(key)).map(month -> BillingPeriod.of(year, month));
    }
}

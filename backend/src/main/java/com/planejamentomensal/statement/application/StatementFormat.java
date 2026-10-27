package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

/**
 * Numbers, dates and periods of a statement in the member's language (I18N-05, I18N-10): R$ 1.234,56 and
 * 26/10/2026 in pt-BR; R$1,234.56 and 10/26/2026 in en-US. The currency is always BRL.
 *
 * @param locale language of the statement
 * @since 1.0
 */
public record StatementFormat(Locale locale) {

    private static final Currency BRL = Currency.getInstance("BRL");

    /**
     * Returns the format of a language tag; anything but {@code en-US} is pt-BR.
     *
     * @param language language tag
     * @return format
     */
    public static StatementFormat of(String language) {
        return new StatementFormat("en-US".equals(language) ? Locale.US : Locale.forLanguageTag("pt-BR"));
    }

    /**
     * Returns the language tag.
     *
     * @return {@code pt-BR} or {@code en-US}
     */
    public String language() {
        return locale.toLanguageTag();
    }

    /**
     * Formats money.
     *
     * @param money amount, may be {@code null}
     * @return text, empty when {@code null}
     */
    public String money(Money money) {
        if (money == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(BRL);
        return format.format(money.amount()).replace(' ', ' ');
    }

    /**
     * Formats a date.
     *
     * @param date date, may be {@code null}
     * @return text, empty when {@code null}
     */
    public String date(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(Locale.US.equals(locale) ? "MM/dd/yyyy" : "dd/MM/yyyy"));
    }

    /**
     * Formats a billing period as month and year, such as "Novembro de 2026" or "November 2026".
     *
     * @param period billing period
     * @return text
     */
    public String period(BillingPeriod period) {
        String text = period.atDay(1).format(DateTimeFormatter.ofPattern(Locale.US.equals(locale) ? "MMMM yyyy" : "MMMM 'de' yyyy", locale));
        return text.substring(0, 1).toUpperCase(locale) + text.substring(1);
    }

    /**
     * Formats a percentage with up to one decimal place.
     *
     * @param value percentage, may be {@code null}
     * @return text such as "50%" or "33,3%", empty when {@code null}
     */
    public String percent(BigDecimal value) {
        if (value == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(1);
        return format.format(value) + "%";
    }
}

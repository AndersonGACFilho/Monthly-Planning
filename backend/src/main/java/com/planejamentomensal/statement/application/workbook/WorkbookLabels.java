package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.time.format.TextStyle;
import java.util.Locale;
import org.springframework.context.MessageSource;

/**
 * Sheet names, titles, headers and month names of the spreadsheet in one language: pt-BR follows the model to the
 * letter, en-US translates them (XLS-12, XLS-15).
 *
 * @param messages labels
 * @param locale   spreadsheet language
 * @since 1.0
 */
public record WorkbookLabels(MessageSource messages, Locale locale) {

    /**
     * Returns a label.
     *
     * @param key       key after {@code workbook.}
     * @param arguments arguments of the label
     * @return text
     */
    public String get(String key, Object... arguments) {
        return messages.getMessage("workbook." + key, arguments.length == 0 ? null : arguments, locale);
    }

    /**
     * Returns the month name in capitals, generated from the calendar, never typed (XLS-12).
     *
     * @param period billing period
     * @return such as {@code NOVEMBRO} or {@code NOVEMBER}
     */
    public String month(BillingPeriod period) {
        return period.yearMonth().getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale).toUpperCase(locale);
    }

    /**
     * Returns the sheet name of a fixed sheet.
     *
     * @param sheet {@code summary}, {@code income}, {@code expenses}, {@code cards} or {@code lists}
     * @return sheet name
     */
    public String sheet(String sheet) {
        return get("sheet." + sheet);
    }
}

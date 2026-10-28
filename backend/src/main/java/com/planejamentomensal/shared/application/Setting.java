package com.planejamentomensal.shared.application;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Every user setting, its default and its validation (IMP-10).
 *
 * @since 1.0
 */
public enum Setting {

    /** Interface language (I18N-03); empty means "follow the system". */
    LANGUAGE("language", "", v -> v.isEmpty() || v.equals("pt-BR") || v.equals("en-US")),
    /** Colour theme of the interface (UI-08); empty means "follow the system". */
    THEME("theme", "", v -> v.isEmpty() || v.equals("light") || v.equals("dark")),
    /** Percentage of income spent that turns the alert yellow (RF-16). */
    ALERT_WARNING_PERCENT("alert-warning-percent", "80", Setting::isPercent),
    /** Percentage of income spent that turns the alert red (RF-16). */
    ALERT_DANGER_PERCENT("alert-danger-percent", "100", Setting::isPercent),
    /** Validate review items automatically when their alias has category and member (RF-79d). */
    AUTO_VALIDATE("auto-validate", "false", Setting::isBoolean),
    /** Months covered by subscription projections (RN-09). */
    PROJECTION_HORIZON_MONTHS("projection-horizon-months", "12", v -> isIntBetween(v, 1, 36)),
    /** Members that get their own sheet in the monthly spreadsheet, as comma-separated ids (XLS-06). */
    WORKBOOK_MEMBER_SHEETS("workbook-member-sheets", "", v -> v.isEmpty() || v.matches("\\d+(,\\d+)*")),
    /** Months before the current one in member sheets (XLS-06). */
    WORKBOOK_MONTHS_BEFORE("workbook-months-before", "2", v -> isIntBetween(v, 0, 12)),
    /** Months after the current one in member sheets (XLS-06). */
    WORKBOOK_MONTHS_AFTER("workbook-months-after", "1", v -> isIntBetween(v, 0, 12)),
    /** Formula mode of the spreadsheet: empty follows the OS (XLS-09). */
    WORKBOOK_FORMULA_MODE("workbook-formula-mode", "", v -> v.isEmpty() || v.equals("EXCEL_365") || v.equals("COMPATIBLE")),
    /** Suffix of the spreadsheet file name, such as {@code CJ} (XLS-01). */
    WORKBOOK_SUFFIX("workbook-suffix", "", v -> v.length() <= 20),
    /** Card order in the spreadsheet, as comma-separated card ids (XLS-03). */
    WORKBOOK_CARD_ORDER("workbook-card-order", "", v -> v.isEmpty() || v.matches("\\d+(,\\d+)*")),
    /** SMTP host for e-mail delivery (RF-86). */
    SMTP_HOST("smtp-host", "", v -> v.length() <= 255),
    /** SMTP port (RF-86). */
    SMTP_PORT("smtp-port", "587", v -> isIntBetween(v, 1, 65535)),
    /** SMTP user name (RF-86). */
    SMTP_USERNAME("smtp-username", "", v -> v.length() <= 255),
    /** Sender address of e-mails (RF-86). */
    SMTP_FROM("smtp-from", "", v -> v.isEmpty() || v.contains("@")),
    /** Use STARTTLS (RF-86). */
    SMTP_STARTTLS("smtp-starttls", "true", Setting::isBoolean);

    private final String key;
    private final String defaultValue;
    private final Predicate<String> validator;

    Setting(String key, String defaultValue, Predicate<String> validator) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    /**
     * Returns the key used in storage and in the API.
     *
     * @return kebab-case key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the value used while the user has not changed it.
     *
     * @return default value as text
     */
    public String defaultValue() {
        return defaultValue;
    }

    /**
     * Tells whether a value is acceptable.
     *
     * @param value candidate value
     * @return {@code true} if valid
     */
    public boolean isValid(String value) {
        return value != null && validator.test(value);
    }

    /**
     * Finds a setting by key.
     *
     * @param key kebab-case key
     * @return the setting, or empty
     */
    public static Optional<Setting> fromKey(String key) {
        return Arrays.stream(values()).filter(s -> s.key.equals(key)).findFirst();
    }

    private static boolean isBoolean(String v) {
        return v.equals("true") || v.equals("false");
    }

    private static boolean isPercent(String v) {
        return isIntBetween(v, 1, 1000);
    }

    private static boolean isIntBetween(String v, int min, int max) {
        try {
            int n = Integer.parseInt(v);
            return n >= min && n <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

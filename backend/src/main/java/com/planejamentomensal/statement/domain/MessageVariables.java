package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variables of template texts, always in English (TPL-05): {@code {{member.name}}}, {@code {{billingPeriod}}},
 * {@code {{total}}}, {@code {{dueDate}}} and {@code {{pixKey}}}.
 *
 * @since 1.0
 */
public final class MessageVariables {

    /** Allowed variable names. */
    public static final Set<String> ALLOWED = Set.of("member.name", "billingPeriod", "total", "dueDate", "pixKey");

    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([A-Za-z.]+)\\s*}}");

    private MessageVariables() {
    }

    /**
     * Lists the unknown variables of a text.
     *
     * @param text text, may be {@code null}
     * @return unknown variable names, in order of appearance
     */
    public static Set<String> unknown(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text == null) {
            return result;
        }
        Matcher m = VARIABLE.matcher(text);
        while (m.find()) {
            if (!ALLOWED.contains(m.group(1))) {
                result.add(m.group(1));
            }
        }
        return result;
    }

    /**
     * Checks a text.
     *
     * @param text text
     * @throws InvalidInputException with code {@code template-variable-unknown} for an unknown variable
     */
    public static void check(String text) {
        Set<String> unknown = unknown(text);
        if (!unknown.isEmpty()) {
            throw new InvalidInputException("template-variable-unknown", String.join(", ", unknown));
        }
    }

    /**
     * Replaces the variables of a text.
     *
     * @param text   text, may be {@code null}
     * @param values value of each allowed variable
     * @return the text with values; unknown variables are left as written
     */
    public static String apply(String text, Map<String, String> values) {
        if (text == null) {
            return "";
        }
        Matcher m = VARIABLE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = ALLOWED.contains(m.group(1)) ? values.getOrDefault(m.group(1), "") : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}

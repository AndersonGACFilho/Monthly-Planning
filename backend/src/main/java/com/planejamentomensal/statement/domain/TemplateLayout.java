package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Placeholders of a full layout written in the advanced template mode (TPL-07): each one stands for a part of the
 * standard statement ({@code {{header}}}, {@code {{warnings}}}, one {@code {{block.<name>}}} per
 * {@link StatementBlock} and {@code {{footer}}}), next to the variables of {@link MessageVariables}.
 *
 * @since 1.0
 */
public final class TemplateLayout {

    /** Placeholder of the standard header, or of the advanced header HTML when there is one. */
    public static final String HEADER = "header";
    /** Placeholder of the fallback and pending warnings. */
    public static final String WARNINGS = "warnings";
    /** Placeholder of the footer text. */
    public static final String FOOTER = "footer";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z.]+)\\s*}}");

    /** Element id of each part in the standard layout, by placeholder name, in the standard order. */
    private static final Map<String, String> ELEMENT_IDS = buildElementIds();

    private TemplateLayout() {
    }

    /**
     * Returns the element id of each part in the standard layout, by placeholder name.
     *
     * @return ids in the standard order: header, warnings, the blocks, footer
     */
    public static Map<String, String> elementIds() {
        return ELEMENT_IDS;
    }

    private static Map<String, String> buildElementIds() {
        Map<String, String> ids = new LinkedHashMap<>();
        ids.put(HEADER, "statement-header");
        ids.put(WARNINGS, "statement-warnings");
        for (StatementBlock block : StatementBlock.values()) {
            ids.put(placeholder(block), elementId(block));
        }
        ids.put(FOOTER, "statement-footer");
        return Collections.unmodifiableMap(ids);
    }

    /**
     * Returns the placeholder name of a block.
     *
     * @param block block
     * @return {@code block.} plus the block name in camel case, such as {@code block.fixedExpenses}
     */
    public static String placeholder(StatementBlock block) {
        String[] words = block.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder name = new StringBuilder("block.").append(words[0]);
        for (int i = 1; i < words.length; i++) {
            name.append(Character.toUpperCase(words[i].charAt(0))).append(words[i].substring(1));
        }
        return name.toString();
    }

    /**
     * Returns the element id of a block in the standard layout.
     *
     * @param block block
     * @return {@code block-} plus the block name in kebab case, such as {@code block-fixed-expenses}
     */
    public static String elementId(StatementBlock block) {
        return "block-" + block.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /**
     * Returns the layout a new full layout starts from: every part in the standard order inside a
     * {@code statement} container.
     *
     * @return HTML with placeholders
     */
    public static String standardLayout() {
        return ELEMENT_IDS.keySet().stream().map(name -> "  {{" + name + "}}")
                .collect(Collectors.joining("\n", "<div class=\"statement\">\n", "\n</div>"));
    }

    /**
     * Checks that a layout only uses known placeholders and variables.
     *
     * @param layout layout HTML, may be {@code null}
     * @throws InvalidInputException with code {@code template-variable-unknown} naming the unknown ones
     */
    public static void check(String layout) {
        if (layout == null) {
            return;
        }
        Set<String> unknown = new LinkedHashSet<>();
        Matcher m = PLACEHOLDER.matcher(layout);
        while (m.find()) {
            String name = m.group(1);
            if (!ELEMENT_IDS.containsKey(name) && !MessageVariables.ALLOWED.contains(name)) {
                unknown.add(name);
            }
        }
        if (!unknown.isEmpty()) {
            throw new InvalidInputException("template-variable-unknown", String.join(", ", unknown));
        }
    }

    /**
     * Replaces, in one pass, the placeholders of the parts and the variables with their values, so a value that
     * itself looks like a placeholder is never replaced again. A part missing from {@code values} becomes empty;
     * any other name is left as written.
     *
     * @param layout layout HTML, already sanitized
     * @param values HTML of each part and escaped value of each variable, by name
     * @return the filled layout
     */
    public static String fill(String layout, Map<String, String> values) {
        Matcher m = PLACEHOLDER.matcher(layout);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            String value = values.getOrDefault(name, ELEMENT_IDS.containsKey(name) ? "" : m.group());
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}

package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.MessageVariables;
import com.planejamentomensal.statement.domain.StatementBlock;
import com.planejamentomensal.statement.domain.TemplateFont;
import com.planejamentomensal.statement.domain.TemplateLayout;
import com.planejamentomensal.statement.domain.TemplateSettings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * What a full layout of the advanced template mode can use (TPL-07): the standard layout to start from, its
 * placeholders, the message variables, every class and id of the standard statement, so extra CSS can target
 * them, and the rules the sanitizer applies, so a template can be written elsewhere (IMP-61). The classes and ids
 * are read from the statement template itself, so the list never drifts from it.
 *
 * @since 1.0
 */
@Component
public class TemplateLayoutGuide {

    private static final String TEMPLATE = "statement/templates/statement.html";
    /** Class names added by Thymeleaf conditions, such as {@code th:classappend="${line.pending} ? 'pending'"}. */
    private static final Pattern APPENDED_CLASS = Pattern.compile("'([A-Za-z][\\w-]*)'");

    /** Rules of the advanced mode; they do not depend on the language. */
    private static final LayoutRules RULES = new LayoutRules(TemplateSanitizer.HTML_TAGS, TemplateSanitizer.LAYOUT_TAGS,
            List.of("class", "style"), List.of("class", "style", "id"), TemplateSanitizer.FORBIDDEN_CSS,
            TemplateSettings.MAX_ADVANCED_LENGTH, TemplateSettings.MAX_ADVANCED_LENGTH, TemplateSettings.MAX_LAYOUT_LENGTH,
            Arrays.stream(TemplateFont.values()).collect(Collectors.toMap(Enum::name, TemplateFont::family,
                    (a, b) -> a, LinkedHashMap::new)),
            Arrays.stream(StatementBlock.values()).map(Enum::name).toList(), TemplateSettings.LANGUAGES);

    private final MessageSource messages;
    private final List<String> selectors;

    /**
     * One class or id of the standard statement.
     *
     * @param selector    CSS selector, {@code .name} or {@code #name}
     * @param description what it marks, in the requested language
     */
    public record LayoutSelector(String selector, String description) {
    }

    /**
     * What the sanitizer and the settings accept, so a template written elsewhere can follow it (TPL-07, IMP-61).
     *
     * @param htmlTags         tags kept in the header HTML and in a full layout
     * @param layoutTags       extra tags a full layout may use
     * @param htmlAttributes   attributes kept in the header HTML
     * @param layoutAttributes attributes kept in a full layout
     * @param forbiddenCss     constructs that make a style be dropped whole
     * @param maxCss           longest extra CSS, in characters
     * @param maxHeaderHtml    longest header HTML, in characters
     * @param maxLayout        longest full layout, in characters
     * @param fonts            font values and their CSS family
     * @param blocks           block values, in the standard order
     * @param languages        languages with their own texts
     */
    public record LayoutRules(List<String> htmlTags, List<String> layoutTags, List<String> htmlAttributes,
            List<String> layoutAttributes, List<String> forbiddenCss, int maxCss, int maxHeaderHtml, int maxLayout,
            Map<String, String> fonts, List<String> blocks, List<String> languages) {
    }

    /**
     * Everything a full layout can use.
     *
     * @param standardLayout layout a new full layout starts from
     * @param placeholders   placeholders of the statement parts, in the standard order
     * @param variables      message variables
     * @param selectors      ids, then classes, in the order they appear
     * @param rules          what the sanitizer and the settings accept
     */
    public record LayoutReference(String standardLayout, List<String> placeholders, List<String> variables,
            List<LayoutSelector> selectors, LayoutRules rules) {
    }


    /**
     * Creates the reference and reads the classes and ids of the statement template.
     *
     * @param messages descriptions of the selectors
     */
    public TemplateLayoutGuide(MessageSource messages) {
        this.messages = messages;
        this.selectors = selectorsOf(readTemplate(), Jsoup.parseBodyFragment(TemplateLayout.standardLayout()));
    }

    /**
     * Returns the reference in a language.
     *
     * @param locale language of the descriptions
     * @return the reference
     */
    public LayoutReference reference(Locale locale) {
        List<LayoutSelector> described = selectors.stream()
                .map(s -> new LayoutSelector(s, messages.getMessage(messageKey(s), null, "", locale)))
                .toList();
        return new LayoutReference(TemplateLayout.standardLayout(), List.copyOf(TemplateLayout.elementIds().keySet()),
                MessageVariables.ALLOWED.stream().sorted().toList(), described, RULES);
    }

    /**
     * Returns the message key that describes a selector.
     *
     * @param selector {@code .name} or {@code #name}
     * @return {@code template.selector.class.name} or {@code template.selector.id.name}
     */
    static String messageKey(String selector) {
        return "template.selector." + (selector.startsWith("#") ? "id." : "class.") + selector.substring(1);
    }

    private static List<String> selectorsOf(Document... documents) {
        Set<String> ids = new LinkedHashSet<>();
        Set<String> classes = new LinkedHashSet<>();
        for (Document document : documents) {
            for (Element element : document.getAllElements()) {
                if (element.hasAttr("id")) {
                    ids.add("#" + element.attr("id"));
                }
                element.classNames().forEach(name -> classes.add("." + name));
                Matcher appended = APPENDED_CLASS.matcher(element.attr("th:classappend"));
                while (appended.find()) {
                    classes.add("." + appended.group(1));
                }
            }
        }
        List<String> all = new ArrayList<>(ids);
        all.addAll(classes);
        return List.copyOf(all);
    }

    private static Document readTemplate() {
        try (InputStream in = TemplateLayoutGuide.class.getClassLoader().getResourceAsStream(TEMPLATE)) {
            if (in == null) {
                throw new IllegalStateException("Missing " + TEMPLATE);
            }
            return Jsoup.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

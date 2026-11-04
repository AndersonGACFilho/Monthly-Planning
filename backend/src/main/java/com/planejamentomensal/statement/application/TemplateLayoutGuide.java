package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.MessageVariables;
import com.planejamentomensal.statement.domain.TemplateLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * What a full layout of the advanced template mode can use (TPL-07): the standard layout to start from, its
 * placeholders, the message variables and every class and id of the standard statement, so extra CSS can target
 * them. The classes and ids are read from the statement template itself, so the list never drifts from it.
 *
 * @since 1.0
 */
@Component
public class TemplateLayoutGuide {

    private static final String TEMPLATE = "statement/templates/statement.html";
    /** Class names added by Thymeleaf conditions, such as {@code th:classappend="${line.pending} ? 'pending'"}. */
    private static final Pattern APPENDED_CLASS = Pattern.compile("'([A-Za-z][\\w-]*)'");

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
     * Everything a full layout can use.
     *
     * @param standardLayout layout a new full layout starts from
     * @param placeholders   placeholders of the statement parts, in the standard order
     * @param variables      message variables
     * @param selectors      ids, then classes, in the order they appear
     */
    public record LayoutReference(String standardLayout, List<String> placeholders, List<String> variables,
            List<LayoutSelector> selectors) {
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
                MessageVariables.ALLOWED.stream().sorted().toList(), described);
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

package com.planejamentomensal.statement.application;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

/**
 * Cleans the HTML and CSS of the advanced template mode: no scripts, no external resources, no event handlers
 * (TPL-07). Variables are replaced afterwards by {@link com.planejamentomensal.statement.domain.MessageVariables}.
 *
 * @since 1.0
 */
public final class TemplateSanitizer {

    /** Tags kept in the header HTML and in a full layout: jsoup's relaxed list without links and images. */
    public static final List<String> HTML_TAGS = List.of("b", "blockquote", "br", "caption", "cite", "code", "col",
            "colgroup", "dd", "div", "dl", "dt", "em", "h1", "h2", "h3", "h4", "h5", "h6", "i", "li", "ol", "p", "pre",
            "q", "small", "span", "strike", "strong", "sub", "sup", "table", "tbody", "td", "tfoot", "th", "thead", "tr",
            "u", "ul");
    /** Sectioning tags a full layout may use on top of {@link #HTML_TAGS}. */
    public static final List<String> LAYOUT_TAGS = List.of("section", "header", "footer", "main", "article", "aside");
    /** CSS constructs that make a style be dropped whole, as the user would write them. */
    public static final List<String> FORBIDDEN_CSS = List.of("@import", "url(...) other than data:image",
            "expression(", "javascript:", "behavior:", "-moz-binding", "</", "<!--");

    private static final Safelist HTML = Safelist.relaxed()
            .removeTags("a", "img")
            .addAttributes(":all", "class", "style");

    /** A full layout may also name its elements, so its CSS can target them. */
    private static final Safelist LAYOUT = Safelist.relaxed()
            .removeTags("a", "img")
            .addTags(LAYOUT_TAGS.toArray(String[]::new))
            .addAttributes(":all", "class", "style", "id");

    private static final Pattern DANGEROUS_CSS = Pattern.compile(
            "@import|expression\\s*\\(|javascript:|behavior\\s*:|-moz-binding|</|<!--|url\\s*\\((?!\\s*['\"]?data:image/)",
            Pattern.CASE_INSENSITIVE);

    private TemplateSanitizer() {
    }

    /**
     * Keeps only safe tags and attributes (structure, text formatting, tables, {@code class} and {@code style});
     * a {@code style} with a dangerous construct is removed.
     *
     * @param html user HTML, may be {@code null}
     * @return safe HTML, empty when {@code null}
     */
    public static String html(String html) {
        return clean(html, HTML);
    }

    /**
     * Cleans a full layout like {@link #html(String)}, also keeping {@code id} attributes and the sectioning tags
     * ({@code section}, {@code header}, {@code footer}, {@code main}, {@code article}, {@code aside}).
     *
     * @param html user layout, may be {@code null}
     * @return safe HTML, empty when {@code null}
     */
    public static String layout(String html) {
        return clean(html, LAYOUT);
    }

    private static String clean(String html, Safelist safelist) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(Jsoup.clean(html, safelist));
        for (Element element : document.select("[style]")) {
            if (css(element.attr("style")).isEmpty()) {
                element.removeAttr("style");
            }
        }
        return document.body().html();
    }

    /**
     * Removes CSS constructs that load resources or run code; a style with any of them is dropped whole.
     *
     * @param css user CSS, may be {@code null}
     * @return safe CSS, empty when {@code null} or dangerous
     */
    public static String css(String css) {
        if (css == null || css.isBlank()) {
            return "";
        }
        return DANGEROUS_CSS.matcher(css.toLowerCase(Locale.ROOT)).find() ? "" : css;
    }
}

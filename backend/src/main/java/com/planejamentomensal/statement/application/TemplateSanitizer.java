package com.planejamentomensal.statement.application;

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

    private static final Safelist HTML = Safelist.relaxed()
            .removeTags("a", "img")
            .addAttributes(":all", "class", "style");

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
        if (html == null || html.isBlank()) {
            return "";
        }
        Document document = Jsoup.parseBodyFragment(Jsoup.clean(html, HTML));
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

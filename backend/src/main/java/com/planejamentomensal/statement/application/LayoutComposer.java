package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.TemplateLayout;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Builds a statement from a full layout of the advanced template mode (TPL-07): the standard statement is rendered
 * with every block, each part is cut out by its element id, and the sanitized layout puts the parts where its
 * placeholders are. The head of the standard statement, with its styles and the extra CSS, is kept.
 *
 * @since 1.0
 */
final class LayoutComposer {

    private LayoutComposer() {
    }

    /**
     * Replaces the body of the standard statement with the layout.
     *
     * @param standard  HTML of the standard statement rendered with every block
     * @param layout    layout written by the user, not yet sanitized
     * @param variables escaped value of each message variable
     * @return the statement HTML
     */
    static String compose(String standard, String layout, Map<String, String> variables) {
        Document page = Jsoup.parse(standard);
        page.outputSettings().prettyPrint(false);
        Map<String, String> values = new HashMap<>(variables);
        TemplateLayout.elementIds().forEach((name, id) -> {
            Element part = page.getElementById(id);
            values.put(name, part == null ? "" : part.outerHtml());
        });
        page.body().html(TemplateLayout.fill(TemplateSanitizer.layout(layout), values));
        return page.outerHtml();
    }
}

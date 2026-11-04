package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What one export changes in its template without saving it (IMP-60): the header and footer texts, the blocks shown
 * and their order, and the columns left out of the line tables. Header and footer replace the template texts in every
 * language and may use the variables of {@link MessageVariables}.
 *
 * @param header        header text of this export, or {@code null} for the template's
 * @param footer        footer text of this export, or {@code null} for the template's
 * @param blocks        blocks shown, in order, or {@code null} for the template's
 * @param hiddenColumns columns left out of the line tables
 * @since 1.0
 */
public record StatementCustomization(String header, String footer, List<StatementBlock> blocks,
        Set<StatementColumn> hiddenColumns) {

    /** No change: the export looks exactly like its template. */
    public static final StatementCustomization NONE = new StatementCustomization(null, null, null, Set.of());

    private static final int MAX_TEXT_LENGTH = 200;

    /**
     * Normalizes the customization: blank texts and an empty block list mean "use the template".
     *
     * @param header        header text, may be {@code null}
     * @param footer        footer text, may be {@code null}
     * @param blocks        blocks, may be {@code null}
     * @param hiddenColumns hidden columns, may be {@code null}
     */
    public StatementCustomization {
        header = header == null || header.isBlank() ? null : header.strip();
        footer = footer == null || footer.isBlank() ? null : footer.strip();
        blocks = blocks == null || blocks.isEmpty() ? null : List.copyOf(blocks);
        hiddenColumns = hiddenColumns == null || hiddenColumns.isEmpty() ? Set.of() : Set.copyOf(hiddenColumns);
    }

    /**
     * Checks the customization.
     *
     * @throws InvalidInputException with code {@code statement-customization-invalid} naming the wrong field, or
     *                               {@code template-variable-unknown}
     */
    public void validate() {
        if (header != null && header.length() > MAX_TEXT_LENGTH) {
            throw new InvalidInputException("statement-customization-invalid", "header");
        }
        if (footer != null && footer.length() > MAX_TEXT_LENGTH) {
            throw new InvalidInputException("statement-customization-invalid", "footer");
        }
        if (blocks != null && EnumSet.copyOf(blocks).size() != blocks.size()) {
            throw new InvalidInputException("statement-customization-invalid", "blocks");
        }
        MessageVariables.check(header);
        MessageVariables.check(footer);
    }

    /**
     * Applies the header, footer and blocks to template settings. With a full layout the blocks still decide what is
     * rendered: a block left out stays empty where its placeholder is.
     *
     * @param settings settings of the template in use
     * @return the settings of this export; the same instance when nothing changes them
     */
    public TemplateSettings applyTo(TemplateSettings settings) {
        if (header == null && footer == null && blocks == null) {
            return settings;
        }
        // Languages without texts keep falling back to the other language, which carries the change too (I18N-11)
        Map<String, TemplateTexts> texts = new LinkedHashMap<>();
        if (settings.texts() != null) {
            settings.texts().forEach((language, own) -> texts.put(language, own == null ? null
                    : new TemplateTexts(header != null ? header : own.header(), footer != null ? footer : own.footer(),
                            own.message(), own.emailSubject(), own.emailBody(), own.whatsapp())));
        }
        if (texts.isEmpty() && (header != null || footer != null)) {
            TemplateSettings.LANGUAGES.forEach(language ->
                    texts.put(language, new TemplateTexts(header, footer, null, null, null, null)));
        }
        return new TemplateSettings(settings.primaryColor(), settings.secondaryColor(), settings.font(), settings.logo(),
                blocks != null ? blocks : settings.blocks(), texts, settings.advancedCss(), settings.advancedHtml(),
                settings.layoutHtml());
    }

    /**
     * Tells whether a column is shown.
     *
     * @param column column
     * @return {@code false} when this export leaves it out
     */
    public boolean shows(StatementColumn column) {
        return !hiddenColumns.contains(column);
    }
}

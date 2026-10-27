package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Look and texts of a statement template, without code (TPL-02, TPL-05, TPL-07, I18N-11).
 *
 * @param primaryColor   primary colour, {@code #RRGGBB}
 * @param secondaryColor secondary colour, {@code #RRGGBB}
 * @param font           font family
 * @param logo           logo as a {@code data:image/png;base64,...} or {@code data:image/jpeg;base64,...} URI, or
 *                       {@code null}
 * @param blocks         blocks shown, in order
 * @param texts          texts by language tag ({@code pt-BR}, {@code en-US})
 * @param advancedCss    extra CSS of the advanced mode, or {@code null} (TPL-07)
 * @param advancedHtml   HTML of the advanced mode that replaces the header, or {@code null}; it is sanitized and
 *                       may hold the variables of {@link MessageVariables} (TPL-07)
 * @since 1.0
 */
public record TemplateSettings(String primaryColor, String secondaryColor, TemplateFont font, String logo,
        List<StatementBlock> blocks, Map<String, TemplateTexts> texts, String advancedCss, String advancedHtml) {

    /** Languages with their own texts (I18N-11). */
    public static final List<String> LANGUAGES = List.of("pt-BR", "en-US");

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern LOGO = Pattern.compile("^data:image/(png|jpeg);base64,[A-Za-z0-9+/=]+$");
    private static final int MAX_LOGO_LENGTH = 700_000;
    private static final int MAX_ADVANCED_LENGTH = 20_000;

    /**
     * Checks the settings.
     *
     * @throws InvalidInputException with code {@code template-invalid} naming the wrong field, or
     *                               {@code template-variable-unknown}
     */
    public void validate() {
        if (primaryColor == null || !COLOR.matcher(primaryColor).matches()) {
            throw new InvalidInputException("template-invalid", "primaryColor");
        }
        if (secondaryColor == null || !COLOR.matcher(secondaryColor).matches()) {
            throw new InvalidInputException("template-invalid", "secondaryColor");
        }
        if (font == null) {
            throw new InvalidInputException("template-invalid", "font");
        }
        if (logo != null && (logo.length() > MAX_LOGO_LENGTH || !LOGO.matcher(logo).matches())) {
            throw new InvalidInputException("template-invalid", "logo");
        }
        if (blocks == null || blocks.isEmpty() || EnumSet.copyOf(blocks).size() != blocks.size()) {
            throw new InvalidInputException("template-invalid", "blocks");
        }
        if (texts == null || !LANGUAGES.containsAll(texts.keySet())) {
            throw new InvalidInputException("template-invalid", "texts");
        }
        if (advancedCss != null && advancedCss.length() > MAX_ADVANCED_LENGTH) {
            throw new InvalidInputException("template-invalid", "advancedCss");
        }
        if (advancedHtml != null && advancedHtml.length() > MAX_ADVANCED_LENGTH) {
            throw new InvalidInputException("template-invalid", "advancedHtml");
        }
        MessageVariables.check(advancedHtml);
        texts.values().forEach(t -> {
            MessageVariables.check(t.header());
            MessageVariables.check(t.footer());
            MessageVariables.check(t.message());
            MessageVariables.check(t.emailSubject());
            MessageVariables.check(t.emailBody());
            MessageVariables.check(t.whatsapp());
        });
    }

    /**
     * Returns the texts of a language, falling back to the other language (I18N-11).
     *
     * @param language language tag
     * @return texts and whether the fallback was used
     */
    public LocalizedTexts textsFor(String language) {
        TemplateTexts own = texts == null ? null : texts.get(language);
        if (own != null && !own.isEmpty()) {
            return new LocalizedTexts(own, false);
        }
        TemplateTexts other = texts == null ? null : texts.entrySet().stream()
                .filter(e -> !e.getKey().equals(language) && e.getValue() != null && !e.getValue().isEmpty())
                .map(Map.Entry::getValue).findFirst().orElse(null);
        return other == null ? new LocalizedTexts(new TemplateTexts(null, null, null, null, null, null), false)
                : new LocalizedTexts(other, true);
    }

    /**
     * Texts of a language.
     *
     * @param texts    texts
     * @param fallback {@code true} when they come from the other language (warn in the preview)
     */
    public record LocalizedTexts(TemplateTexts texts, boolean fallback) {
    }
}

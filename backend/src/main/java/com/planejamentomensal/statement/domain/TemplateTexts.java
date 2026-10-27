package com.planejamentomensal.statement.domain;

/**
 * Customizable texts of a template in one language (TPL-02, TPL-05, I18N-11). Fields may hold the
 * variables of {@link MessageVariables}.
 *
 * @param header       header text
 * @param footer       footer text
 * @param message      default message shown on the statement
 * @param emailSubject e-mail subject
 * @param emailBody    e-mail body
 * @param whatsapp     WhatsApp message
 * @since 1.0
 */
public record TemplateTexts(String header, String footer, String message, String emailSubject, String emailBody,
        String whatsapp) {

    /**
     * Tells whether no text was written.
     *
     * @return {@code true} if every field is blank
     */
    public boolean isEmpty() {
        return blank(header) && blank(footer) && blank(message) && blank(emailSubject) && blank(emailBody) && blank(whatsapp);
    }

    private static boolean blank(String text) {
        return text == null || text.isBlank();
    }
}

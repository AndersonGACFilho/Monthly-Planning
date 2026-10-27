package com.planejamentomensal.statement.api;

/**
 * Rendered preview of a template (TPL-03, I18N-11).
 *
 * @param html         statement HTML
 * @param subject      e-mail subject
 * @param emailText    e-mail text
 * @param whatsappText WhatsApp message
 * @param fallback     {@code true} when the texts of the other language were used
 * @since 1.0
 */
public record TemplatePreviewModel(String html, String subject, String emailText, String whatsappText, boolean fallback) {
}

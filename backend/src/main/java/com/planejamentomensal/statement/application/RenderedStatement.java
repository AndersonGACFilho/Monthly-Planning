package com.planejamentomensal.statement.application;

/**
 * A statement rendered with a template in one language (TPL-01).
 *
 * @param html         HTML for the e-mail body, the preview, the PDF and the PNG
 * @param subject      e-mail subject
 * @param emailText    plain-text e-mail body
 * @param whatsappText WhatsApp message
 * @param language     language used
 * @param fallback     {@code true} when the template had no texts in that language (I18N-11)
 * @param fileBaseName base name for exported files, such as "extrato-m-2026-11"
 * @since 1.0
 */
public record RenderedStatement(String html, String subject, String emailText, String whatsappText, String language,
        boolean fallback, String fileBaseName) {
}

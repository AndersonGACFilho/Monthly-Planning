package com.planejamentomensal.statement.application;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.planejamentomensal.statement.domain.TemplateFont;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.stereotype.Component;

/**
 * Converts statement HTML into a PDF with the template fonts embedded, so the file looks the same on every
 * system (TPL-01, POR-05).
 *
 * @since 1.0
 */
@Component
public class PdfGenerator {

    private final StatementFiles files;

    /**
     * Creates the generator.
     *
     * @param files fonts folder
     */
    public PdfGenerator(StatementFiles files) {
        this.files = files;
    }

    /**
     * Renders HTML as PDF.
     *
     * @param html HTML5 document
     * @return PDF bytes
     */
    public byte[] toPdf(String html) {
        Path fonts = files.fonts();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            for (TemplateFont font : TemplateFont.values()) {
                builder.useFont(fonts.resolve(font.filePrefix() + "-Regular.ttf").toFile(), font.family(), 400,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
                builder.useFont(fonts.resolve(font.filePrefix() + "-Bold.ttf").toFile(), font.family(), 700,
                        PdfRendererBuilder.FontStyle.NORMAL, true);
            }
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html)), "/");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

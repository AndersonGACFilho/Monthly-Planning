package com.planejamentomensal.statement.application;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/**
 * Renders the statement PDF as one PNG, pages stacked, for pasting in WhatsApp (TPL-01, RF-84).
 *
 * @since 1.0
 */
@Component
public class ImageGenerator {

    /** Resolution of the image. */
    static final float DPI = 110;

    /**
     * Creates the generator.
     */
    public ImageGenerator() {
        // stateless
    }

    /**
     * Converts a PDF into one PNG.
     *
     * @param pdf PDF bytes
     * @return PNG bytes
     */
    public byte[] toPng(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDFRenderer renderer = new PDFRenderer(document);
            List<BufferedImage> pages = new ArrayList<>();
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                pages.add(renderer.renderImageWithDPI(i, DPI, ImageType.RGB));
            }
            int width = pages.stream().mapToInt(BufferedImage::getWidth).max().orElse(1);
            int height = pages.stream().mapToInt(BufferedImage::getHeight).sum();
            BufferedImage image = new BufferedImage(width, Math.max(height, 1), BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            int y = 0;
            for (BufferedImage page : pages) {
                graphics.drawImage(page, 0, y, null);
                y += page.getHeight();
            }
            graphics.dispose();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package com.planejamentomensal.statement.infra;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.application.DataFolders;
import com.planejamentomensal.shared.domain.FileNameSanitizer;
import com.planejamentomensal.statement.application.StatementFiles;
import com.planejamentomensal.statement.domain.TemplateFont;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.stereotype.Component;

/**
 * Keeps statement fonts in {@code data/templates/fonts/} and exports in {@code data/exports/<period>/}.
 *
 * @since 1.0
 */
@Component
public class DataFolderStatementFiles implements StatementFiles {

    private static final String[] WEIGHTS = {"Regular", "Bold"};

    private final DataFolders paths;

    /**
     * Creates the adapter.
     *
     * @param paths portable folders
     */
    public DataFolderStatementFiles(DataFolders paths) {
        this.paths = paths;
    }

    @Override
    public Path fonts() {
        Path dir = paths.templates().resolve("fonts");
        try {
            Files.createDirectories(dir);
            for (TemplateFont font : TemplateFont.values()) {
                for (String weight : WEIGHTS) {
                    String name = font.filePrefix() + "-" + weight + ".ttf";
                    Path target = dir.resolve(name);
                    if (Files.notExists(target)) {
                        try (InputStream in = getClass().getResourceAsStream("/statement/fonts/" + name)) {
                            if (in == null) {
                                throw new IllegalStateException("Missing bundled font " + name);
                            }
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Path saveExport(BillingPeriod period, String baseName, String extension, byte[] content) {
        Path dir = paths.exports().resolve(period.toString());
        try {
            Files.createDirectories(dir);
            Path file = dir.resolve(FileNameSanitizer.fileName(baseName, extension));
            Files.write(file, content);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

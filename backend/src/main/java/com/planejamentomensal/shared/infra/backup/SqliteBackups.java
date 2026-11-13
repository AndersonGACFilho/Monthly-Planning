package com.planejamentomensal.shared.infra.backup;

import com.planejamentomensal.shared.application.Backups;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.infra.storage.AppPaths;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Component;

/**
 * SQLite copies made with {@code VACUUM INTO}, consistent even while the app is writing (POR-12). The copy runs on
 * its own connection because SQLite cannot vacuum inside a transaction.
 *
 * @since 1.0
 */
@Component
public class SqliteBackups implements Backups {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss-SSS");
    private static final String PREFIX = "planejamento-mensal-";
    private static final String IMPORTED = "-imported.db";

    private final DataSource dataSource;
    private final AppPaths paths;
    private final Clock clock;

    /**
     * Creates the backups.
     *
     * @param dataSource database
     * @param paths      portable folders
     * @param clock      clock for the file names
     */
    public SqliteBackups(DataSource dataSource, AppPaths paths, Clock clock) {
        this.dataSource = dataSource;
        this.paths = paths;
        this.clock = clock;
    }

    @Override
    public Path backup(String reason) {
        Path dir = paths.backups();
        Path file = dir.resolve(PREFIX + LocalDateTime.now(clock).format(STAMP) + "-" + reason.replaceAll("[^a-z0-9-]", "") + ".db");
        copyTo(file);
        list().stream().filter(old -> !isImported(old)).skip(KEPT).forEach(this::delete);
        return file;
    }

    @Override
    public Path backupTo(Path destination) {
        copyTo(destination.toAbsolutePath());
        return destination.toAbsolutePath();
    }

    @Override
    public byte[] copy() {
        Path file = paths.temp().resolve("export-" + UUID.randomUUID() + ".db");
        try {
            copyTo(file);
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(file);
        }
    }

    @Override
    public Path importCopy(byte[] content) {
        Path staged = paths.temp().resolve("import-" + UUID.randomUUID() + ".db");
        try {
            Files.write(staged, content);
            if (!SqliteFileCheck.isAppDatabase(staged)) {
                throw new InvalidInputException("database-file-invalid");
            }
            Path file = paths.backups().resolve(PREFIX + LocalDateTime.now(clock).format(STAMP) + IMPORTED);
            Files.createDirectories(file.getParent());
            return Files.move(staged, file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(staged);
        }
    }

    @Override
    public void delete(Path backup) {
        try {
            Files.deleteIfExists(backup);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isImported(Path backup) {
        return backup.getFileName().toString().endsWith(IMPORTED);
    }

    @Override
    public List<Path> list() {
        Path dir = paths.backups();
        if (Files.notExists(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().startsWith(PREFIX) && p.toString().endsWith(".db"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // a leftover in data/tmp is removed with the folder; the export itself succeeded
        }
    }

    private void copyTo(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + file.toString().replace("'", "''") + "'");
        } catch (SQLException e) {
            throw new DataAccessResourceFailureException("Backup failed: " + e.getMessage(), e);
        }
    }
}

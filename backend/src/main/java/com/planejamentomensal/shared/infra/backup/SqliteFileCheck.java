package com.planejamentomensal.shared.infra.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import org.sqlite.SQLiteConfig;

/**
 * Tells whether a file is an intact SQLite database made by this app, before it is kept as an imported backup
 * (IMP-72): the SQLite header, {@code PRAGMA integrity_check} and the Flyway history the app writes on every database.
 *
 * @since 1.0
 */
final class SqliteFileCheck {

    private static final byte[] HEADER = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private SqliteFileCheck() {
    }

    /**
     * Checks a file, opening it read-only.
     *
     * @param file candidate file
     * @return {@code true} for an intact database of this app
     */
    static boolean isAppDatabase(Path file) {
        return hasHeader(file) && passesChecks(file);
    }

    private static boolean hasHeader(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            return Arrays.equals(in.readNBytes(HEADER.length), HEADER);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean passesChecks(Path file) {
        SQLiteConfig config = new SQLiteConfig();
        config.setReadOnly(true);
        try (Connection connection = config.createConnection("jdbc:sqlite:" + file);
                Statement statement = connection.createStatement()) {
            return "ok".equals(single(statement, "PRAGMA integrity_check"))
                    && "1".equals(single(statement,
                            "SELECT count(*) FROM sqlite_master WHERE type = 'table' AND name = 'flyway_schema_history'"));
        } catch (SQLException e) {
            return false;
        }
    }

    private static String single(Statement statement, String sql) throws SQLException {
        try (ResultSet rows = statement.executeQuery(sql)) {
            return rows.next() ? rows.getString(1) : null;
        }
    }
}

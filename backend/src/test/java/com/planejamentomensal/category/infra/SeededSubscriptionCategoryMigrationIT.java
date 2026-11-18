package com.planejamentomensal.category.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class SeededSubscriptionCategoryMigrationIT {

    @TempDir
    Path dir;

    @Test
    @DisplayName("IMP-65: updating marks the seeded subscription category created before the mark existed")
    void marksSeededCategory() throws SQLException {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + dir.resolve("old.db"));
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("11").load().migrate();
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO category (name, color, icon) VALUES "
                    + "('Assinaturas', '#6366F1', 'repeat'), ('Mercado', '#22C55E', 'cart')");
        }

        Flyway.configure().dataSource(source).locations("classpath:db/migration").load().migrate();

        assertThat(subscriptionMarks(source)).containsExactlyInAnyOrderEntriesOf(Map.of("Assinaturas", true, "Mercado", false));
    }

    private static Map<String, Boolean> subscriptionMarks(SQLiteDataSource source) throws SQLException {
        Map<String, Boolean> marks = new HashMap<>();
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT name, subscription FROM category")) {
            while (rows.next()) {
                marks.put(rows.getString("name"), rows.getBoolean("subscription"));
            }
        }
        return marks;
    }
}

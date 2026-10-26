package com.planejamentomensal.shared.infra.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.PlanejamentoMensalApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Starts the real application on a temporary app folder to check the database file and its settings.
 */
class StorageIT {

    @TempDir
    Path home;

    private ConfigurableApplicationContext start() {
        // Same way the shell passes them (POR-08): command-line arguments win over application.yml
        return new SpringApplicationBuilder(PlanejamentoMensalApplication.class)
                .run("--app.home=" + home, "--server.port=0");
    }

    @Test
    @DisplayName("IMP-03: Flyway migrates the database created under data/")
    void flywayMigratesDatabaseUnderData() {
        try (ConfigurableApplicationContext context = start()) {
            // Given the app started on an empty folder
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
            // When the migration history is read
            Integer applied = jdbc.queryForObject(
                    "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);
            // Then the baseline ran in the file inside data/
            assertThat(applied).isEqualTo(1);
            assertThat(home.resolve("data/planejamento-mensal.db")).isRegularFile();
        }
    }

    @Test
    @DisplayName("POR-11: connections use WAL, synchronous FULL and foreign keys")
    void connectionsUseDurableSettings() {
        try (ConfigurableApplicationContext context = start()) {
            // Given
            JdbcTemplate jdbc = new JdbcTemplate(context.getBean(javax.sql.DataSource.class));
            // When / Then
            assertThat(jdbc.queryForObject("PRAGMA journal_mode", String.class)).isEqualToIgnoringCase("wal");
            assertThat(jdbc.queryForObject("PRAGMA synchronous", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("POR-11: closing the app leaves no pending WAL content")
    void closingLeavesSingleConsistentFile() throws Exception {
        // Given the app wrote data
        try (ConfigurableApplicationContext context = start()) {
            new JdbcTemplate(context.getBean(javax.sql.DataSource.class))
                    .execute("UPDATE schema_info SET created_at = created_at");
        }
        // When it was closed (try-with-resources above)
        Path wal = home.resolve("data/planejamento-mensal.db-wal");
        // Then the WAL file is gone or empty
        assertThat(!Files.exists(wal) || Files.size(wal) == 0).isTrue();
    }
}

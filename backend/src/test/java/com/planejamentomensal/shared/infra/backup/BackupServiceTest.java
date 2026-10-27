package com.planejamentomensal.shared.infra.backup;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.application.Backups;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackupServiceTest {

    @Autowired
    private Backups backups;

    @TempDir
    Path dir;

    @Test
    @DisplayName("POR-12: backups are consistent SQLite copies in data/backups, newest first, keeping the latest ten")
    void keepsTen() throws Exception {
        for (int i = 0; i < Backups.KEPT + 2; i++) {
            backups.backup("manual");
        }
        List<Path> files = backups.list();
        assertThat(files).hasSize(Backups.KEPT);
        assertThat(files.getFirst().getFileName().toString()).startsWith("planejamento-mensal-").endsWith("-manual.db");
        assertThat(files.getFirst().getParent().getFileName().toString()).isEqualTo("backups");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + files.getFirst());
                ResultSet rs = c.createStatement().executeQuery("SELECT count(*) FROM flyway_schema_history")) {
            assertThat(rs.getInt(1)).isGreaterThan(5);
        }
    }

    @Test
    @DisplayName("POR-12: Backup now can write the copy where the user chose")
    void chosenDestination() throws Exception {
        Path target = backups.backupTo(dir.resolve("my-copy.db"));
        assertThat(Files.size(target)).isGreaterThan(0);
    }
}

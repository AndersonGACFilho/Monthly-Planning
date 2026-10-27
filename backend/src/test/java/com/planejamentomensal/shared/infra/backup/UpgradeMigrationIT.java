package com.planejamentomensal.shared.infra.backup;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.application.Backups;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class UpgradeMigrationIT {

    @TempDir
    Path dir;

    @Test
    @DisplayName("POR-14: an update backs up the old database, then Flyway migrates it to the new version")
    void backsUpBeforeMigrating() {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + dir.resolve("old.db"));
        Flyway.configure().dataSource(source).locations("classpath:db/migration").target("5").load().migrate();
        List<String> reasons = new ArrayList<>();
        Backups recorder = new Backups() {
            @Override
            public Path backup(String reason) {
                reasons.add(reason);
                return dir.resolve(reason + ".db");
            }

            @Override
            public Path backupTo(Path destination) {
                return destination;
            }

            @Override
            public List<Path> list() {
                return List.of();
            }
        };
        Flyway latest = Flyway.configure().dataSource(source).locations("classpath:db/migration").load();
        new BackupBeforeMigration().backupThenMigrate(recorder).migrate(latest);
        assertThat(reasons).containsExactly("before-update");
        assertThat(latest.info().pending()).isEmpty();
        new BackupBeforeMigration().backupThenMigrate(recorder).migrate(latest);
        assertThat(reasons).containsExactly("before-update", "start");

        SQLiteDataSource fresh = new SQLiteDataSource();
        fresh.setUrl("jdbc:sqlite:" + dir.resolve("new.db"));
        new BackupBeforeMigration().backupThenMigrate(recorder)
                .migrate(Flyway.configure().dataSource(fresh).locations("classpath:db/migration").load());
        assertThat(reasons).hasSize(2);
    }
}

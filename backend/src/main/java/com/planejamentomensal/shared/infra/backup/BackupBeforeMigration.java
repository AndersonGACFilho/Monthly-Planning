package com.planejamentomensal.shared.infra.backup;

import com.planejamentomensal.shared.application.Backups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backs the database up on every start, before Flyway migrates it, so an update that changes the schema can always be
 * undone by restoring the copy (POR-12, POR-14). A brand-new database has nothing to copy.
 *
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
public class BackupBeforeMigration {

    private static final Logger LOG = LoggerFactory.getLogger(BackupBeforeMigration.class);

    /**
     * Creates the configuration.
     */
    public BackupBeforeMigration() {
        // configuration holder
    }

    /**
     * Migration strategy: backup, then migrate.
     *
     * @param backups backups
     * @return strategy
     */
    @Bean
    public FlywayMigrationStrategy backupThenMigrate(Backups backups) {
        return flyway -> {
            if (flyway.info().applied().length > 0) {
                String reason = flyway.info().pending().length > 0 ? "before-update" : "start";
                LOG.info("Backup on start: {}", backups.backup(reason).getFileName());
            }
            flyway.migrate();
        };
    }
}

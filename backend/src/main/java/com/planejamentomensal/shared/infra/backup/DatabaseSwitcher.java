package com.planejamentomensal.shared.infra.backup;

import com.planejamentomensal.shared.application.ActiveDatabase;
import com.planejamentomensal.shared.application.Backups;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.infra.storage.AppPaths;
import com.planejamentomensal.shared.infra.storage.SqlitePools;
import com.planejamentomensal.shared.infra.storage.SwitchableDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Switches the app between the live database and copies of its backups (IMP-58). A backup is never opened in place:
 * it is copied to {@code data/tmp/}, migrated with the app's Flyway settings (POR-14) and only then put in use, so a
 * backup that fails to migrate leaves the app where it was. Restoring stages the backup the same way, backs the live
 * data up and swaps the file while no connection is open.
 *
 * @since 1.0
 */
@Component
public class DatabaseSwitcher implements ActiveDatabase {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseSwitcher.class);
    private static final String VIEW_PREFIX = "backup-view-";
    private static final String BACKUP_POOL = "sqlite-backup";
    /** SQLite files that travel with a database in WAL mode. */
    private static final List<String> SIDE_FILES = List.of("-wal", "-shm");

    private final SwitchableDataSource dataSource;
    private final AppPaths paths;
    private final Backups backups;
    private final Flyway flyway;
    private String openBackup;
    private Path openCopy;

    /**
     * Creates the switcher and removes copies left by a previous run that closed with a backup open.
     *
     * @param dataSource data source the whole app uses
     * @param paths      portable folders
     * @param backups    backups of {@code data/backups/}
     * @param flyway     migrations of the live database, whose settings migrate the copies too
     */
    public DatabaseSwitcher(SwitchableDataSource dataSource, AppPaths paths, Backups backups, Flyway flyway) {
        this.dataSource = dataSource;
        this.paths = paths;
        this.backups = backups;
        this.flyway = flyway;
        removeLeftoverCopies();
    }

    @Override
    public synchronized State state() {
        return new State(openBackup);
    }

    @Override
    public synchronized State openBackup(String fileName) {
        Path backup = find(fileName);
        Path copy = paths.temp().resolve(VIEW_PREFIX + System.nanoTime() + ".db");
        copy(backup, copy);
        HikariDataSource pool = migrated(copy);
        Path previous = openCopy;
        dataSource.resume(pool);
        openBackup = fileName;
        openCopy = copy;
        deleteDatabase(previous);
        LOG.info("Opened a copy of backup {}", fileName);
        return state();
    }

    @Override
    public synchronized State openLive() {
        if (openBackup == null) {
            return state();
        }
        dataSource.resume(SqlitePools.open(paths.database(), SqlitePools.LIVE));
        deleteDatabase(openCopy);
        LOG.info("Back to the live data after checking backup {}", openBackup);
        openBackup = null;
        openCopy = null;
        return state();
    }

    @Override
    public synchronized State restore(String fileName) {
        Path backup = find(fileName);
        openLive();
        // Staged before the safety backup: making it may remove the oldest backup, which may be this one
        Path staged = paths.temp().resolve("restore-" + System.nanoTime() + ".db");
        copy(backup, staged);
        migrated(staged).close();
        LOG.info("Backup before restore: {}", backups.backup("before-restore").getFileName());
        dataSource.suspend();
        try {
            // A journal left beside the restored file would be replayed into it
            for (String suffix : SIDE_FILES) {
                Files.deleteIfExists(paths.database().resolveSibling(paths.database().getFileName() + suffix));
            }
            Files.move(staged, paths.database(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot restore " + fileName, e);
        } finally {
            dataSource.resume(SqlitePools.open(paths.database(), SqlitePools.LIVE));
        }
        LOG.info("Restored backup {}", fileName);
        return state();
    }

    private Path find(String fileName) {
        return backups.list().stream().filter(p -> p.getFileName().toString().equals(fileName)).findFirst()
                .orElseThrow(() -> new NotFoundException("backup", fileName));
    }

    private HikariDataSource migrated(Path database) {
        HikariDataSource pool = SqlitePools.open(database, BACKUP_POOL);
        try {
            Flyway.configure().configuration(flyway.getConfiguration()).dataSource(pool).load().migrate();
            return pool;
        } catch (RuntimeException e) {
            pool.close();
            deleteDatabase(database);
            throw e;
        }
    }

    private static void copy(Path from, Path to) {
        try {
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot copy " + from.getFileName(), e);
        }
    }

    private void removeLeftoverCopies() {
        try (Stream<Path> files = Files.list(paths.temp())) {
            files.filter(p -> p.getFileName().toString().startsWith(VIEW_PREFIX)).forEach(DatabaseSwitcher::deleteFile);
        } catch (IOException e) {
            LOG.warn("Cannot list {}", paths.temp(), e);
        }
    }

    private static void deleteDatabase(Path database) {
        if (database == null) {
            return;
        }
        deleteFile(database);
        deleteSideFiles(database);
    }

    private static void deleteSideFiles(Path database) {
        SIDE_FILES.forEach(suffix -> deleteFile(database.resolveSibling(database.getFileName() + suffix)));
    }

    private static void deleteFile(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.warn("Cannot delete {}", file, e);
        }
    }
}

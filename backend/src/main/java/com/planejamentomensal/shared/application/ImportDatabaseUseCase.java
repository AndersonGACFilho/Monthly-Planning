package com.planejamentomensal.shared.application;

import com.planejamentomensal.shared.domain.InvalidInputException;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Imports a database file to check it (IMP-72): the file is kept as an imported backup and opened as a copy, like any
 * backup (IMP-58), so the live data never change until the user restores it.
 *
 * @since 1.0
 */
@Service
public class ImportDatabaseUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(ImportDatabaseUseCase.class);

    private final Backups backups;
    private final ActiveDatabase database;

    /**
     * Creates the use case.
     *
     * @param backups  backups, where the file is kept
     * @param database database in use, switched to the imported copy
     */
    public ImportDatabaseUseCase(Backups backups, ActiveDatabase database) {
        this.backups = backups;
        this.database = database;
    }

    /**
     * Imports a database file and opens it to check.
     *
     * @param content the SQLite file
     * @return the database now in use: a copy of the imported file
     * @throws InvalidInputException with code {@code database-file-invalid} when the file is not an intact database of
     *         this app or cannot be brought to this version; nothing is kept then
     */
    public ActiveDatabase.State importDatabase(byte[] content) {
        Path kept = backups.importCopy(content);
        try {
            return database.openBackup(kept.getFileName().toString());
        } catch (RuntimeException e) {
            LOG.warn("Imported database {} could not be opened", kept.getFileName(), e);
            backups.delete(kept);
            throw new InvalidInputException("database-file-invalid");
        }
    }
}

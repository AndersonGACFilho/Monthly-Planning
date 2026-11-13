package com.planejamentomensal.shared.application;

import java.nio.file.Path;
import java.util.List;

/**
 * Copies of the database for safety: one on every start, before migrations, keeping the latest ten, plus copies on
 * request (POR-12, POR-14).
 *
 * @since 1.0
 */
public interface Backups {

    /** Number of automatic backups kept in {@code data/backups/}. */
    int KEPT = 10;

    /**
     * Writes a consistent copy of the database into {@code data/backups/} and removes the oldest beyond
     * {@link #KEPT}.
     *
     * @param reason short tag in the file name, such as {@code start} or {@code manual}
     * @return the copy
     */
    Path backup(String reason);

    /**
     * Writes a consistent copy of the database to a place the user chose.
     *
     * @param destination file path
     * @return the copy
     */
    Path backupTo(Path destination);

    /**
     * Makes a consistent copy of the database to hand to the user (IMP-69); nothing is kept in {@code data/}.
     *
     * @return the SQLite file
     */
    byte[] copy();

    /**
     * Keeps a database file the user imported in {@code data/backups/}, out of the automatic cleanup (IMP-72).
     *
     * @param content the SQLite file
     * @return the kept file
     * @throws com.planejamentomensal.shared.domain.InvalidInputException with code {@code database-file-invalid} when
     *         it is not an intact database of this app; nothing is kept then
     */
    Path importCopy(byte[] content);

    /**
     * Removes a file from {@code data/backups/}.
     *
     * @param backup the file
     */
    void delete(Path backup);

    /**
     * Lists the copies in {@code data/backups/}, newest first.
     *
     * @return files
     */
    List<Path> list();
}

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
     * Lists the copies in {@code data/backups/}, newest first.
     *
     * @return files
     */
    List<Path> list();
}

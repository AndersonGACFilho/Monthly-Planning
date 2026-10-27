package com.planejamentomensal.shared.application;

import java.nio.file.Path;

/**
 * Folders inside the portable {@code data/} directory that modules may write to (POR-03, POR-04).
 *
 * @since 1.0
 */
public interface DataFolders {

    /**
     * Returns {@code data/}.
     *
     * @return data folder
     */
    Path data();

    /**
     * Returns {@code data/templates/}.
     *
     * @return templates folder
     */
    Path templates();

    /**
     * Returns {@code data/exports/}.
     *
     * @return exports folder
     */
    Path exports();
}

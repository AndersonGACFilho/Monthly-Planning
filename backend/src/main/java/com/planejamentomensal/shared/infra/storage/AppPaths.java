package com.planejamentomensal.shared.infra.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Every folder and file the app writes, resolved against the portable app folder.
 *
 * <p>Implements POR-03: nothing is written outside the folder where the executables live. The shell
 * passes that folder as {@code app.home}; the {@code data/} tree below it is shared by Windows and
 * Linux (POR-01).</p>
 *
 * @param home absolute, normalized app folder
 * @since 1.0
 */
public record AppPaths(Path home) {

    /** Name of the SQLite database file inside {@code data/}. */
    public static final String DATABASE_FILE_NAME = "planejamento-mensal.db";

    /**
     * Creates the paths for an app folder.
     *
     * @param home app folder; relative paths are resolved against the working directory
     * @throws NullPointerException if {@code home} is {@code null}
     */
    public AppPaths {
        home = Objects.requireNonNull(home, "home").toAbsolutePath().normalize();
    }

    /**
     * Returns the folder shared between operating systems.
     *
     * @return {@code <home>/data}
     */
    public Path data() {
        return home.resolve("data");
    }

    /**
     * Returns the SQLite database file.
     *
     * @return {@code <home>/data/planejamento-mensal.db}
     */
    public Path database() {
        return data().resolve(DATABASE_FILE_NAME);
    }

    /**
     * Returns the folder with statement templates, logos, fonts and the spreadsheet model.
     *
     * @return {@code <home>/data/templates}
     */
    public Path templates() {
        return data().resolve("templates");
    }

    /**
     * Returns the folder where exported statements and spreadsheets are saved.
     *
     * @return {@code <home>/data/exports}
     */
    public Path exports() {
        return data().resolve("exports");
    }

    /**
     * Returns the folder with automatic and manual backups.
     *
     * @return {@code <home>/data/backups}
     */
    public Path backups() {
        return data().resolve("backups");
    }

    /**
     * Returns the log folder.
     *
     * @return {@code <home>/data/logs}
     */
    public Path logs() {
        return data().resolve("logs");
    }

    /**
     * Returns the folder for temporary files, such as uploads being processed.
     *
     * @return {@code <home>/data/tmp}
     */
    public Path temp() {
        return data().resolve("tmp");
    }

    /**
     * Creates every folder of the {@code data/} tree that does not exist yet.
     *
     * @throws UncheckedIOException if a folder cannot be created, for example when the drive was removed
     */
    public void createDirectories() {
        for (Path dir : List.of(data(), templates(), exports(), backups(), logs(), temp())) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot create " + dir, e);
            }
        }
    }

    /**
     * Tells whether a path stays inside the app folder.
     *
     * @param path path to check; relative paths are resolved against {@link #home()}
     * @return {@code true} if the normalized path is {@link #home()} or below it
     */
    public boolean contains(Path path) {
        return home.resolve(path).toAbsolutePath().normalize().startsWith(home);
    }
}

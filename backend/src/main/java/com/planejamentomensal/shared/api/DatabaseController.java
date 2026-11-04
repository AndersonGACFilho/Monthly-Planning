package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.application.ActiveDatabase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Database the app works on: the live data or a copy of a backup opened to check it, and restoring a backup as the
 * live data (IMP-58, POR-12).
 *
 * @since 1.0
 */
@RestController
public class DatabaseController {

    private final ActiveDatabase database;

    /**
     * Creates the controller.
     *
     * @param database active database
     */
    public DatabaseController(ActiveDatabase database) {
        this.database = database;
    }

    /**
     * Database the app works on.
     *
     * @param live   {@code true} for the live data
     * @param backup file name of the backup open, or {@code null}
     */
    public record DatabaseModel(boolean live, String backup) {

        static DatabaseModel of(ActiveDatabase.State state) {
            return new DatabaseModel(state.live(), state.backup());
        }
    }

    /**
     * Which database to work on.
     *
     * @param backup file name of a backup in {@code data/backups/}, or empty for the live data
     */
    public record DatabaseRequest(@Size(max = 255) String backup) {
    }

    /**
     * Backup to restore.
     *
     * @param backup file name of a backup in {@code data/backups/}
     */
    public record RestoreRequest(@NotBlank @Size(max = 255) String backup) {
    }

    /**
     * Returns the database the app works on.
     *
     * @return live data or the backup open
     */
    @GetMapping("/api/database")
    public DatabaseModel database() {
        return DatabaseModel.of(database.state());
    }

    /**
     * Opens a copy of a backup to check it, or goes back to the live data; the backup file is never changed and what
     * is changed in the copy is dropped when the live data comes back.
     *
     * @param request backup to open, or empty for the live data
     * @return the database now in use
     */
    @PutMapping("/api/database")
    public DatabaseModel switchDatabase(@Valid @RequestBody DatabaseRequest request) {
        boolean live = request.backup() == null || request.backup().isBlank();
        return DatabaseModel.of(live ? database.openLive() : database.openBackup(request.backup()));
    }

    /**
     * Makes a backup the live data, after backing the live data up.
     *
     * @param request backup to restore
     * @return the database now in use: the live data
     */
    @PostMapping("/api/database/restores")
    public DatabaseModel restoreBackup(@Valid @RequestBody RestoreRequest request) {
        return DatabaseModel.of(database.restore(request.backup()));
    }

    /**
     * Adds {@code database} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor databaseRootLink() {
        return () -> Link.of("/api/database", "database");
    }
}

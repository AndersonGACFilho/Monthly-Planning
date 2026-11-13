package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.application.ActiveDatabase;
import com.planejamentomensal.shared.application.Backups;
import com.planejamentomensal.shared.application.DataExport;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Backups of the database and export of all data (POR-12, RNF-07, IMP-69).
 *
 * @since 1.0
 */
@RestController
public class BackupController {

    private final Backups backups;
    private final DataExport export;
    private final ActiveDatabase database;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param backups  backups
     * @param export   data export
     * @param database database in use, so a backup is never made of an open backup
     * @param clock    clock for the date in the database file name
     */
    public BackupController(Backups backups, DataExport export, ActiveDatabase database, Clock clock) {
        this.backups = backups;
        this.export = export;
        this.database = database;
        this.clock = clock;
    }

    /**
     * Destination of a manual backup.
     *
     * @param destination file chosen by the user, or empty for {@code data/backups/}
     */
    public record BackupRequest(@Size(max = 1000) String destination) {
    }

    /**
     * Lists the copies in {@code data/backups/}, newest first.
     *
     * @return file names
     */
    @GetMapping("/api/backups")
    public List<String> backups() {
        return backups.list().stream().map(p -> p.getFileName().toString()).toList();
    }

    /**
     * Makes a backup now, in {@code data/backups/} or where the user chose (POR-12). Refused while a backup is open
     * (IMP-58): the copy would be saved as if it were the live data.
     *
     * @param request destination
     * @return {@code {"file": path}}
     * @throws BusinessRuleException with code {@code backup-open} while a backup is open
     */
    @PostMapping("/api/backups")
    public Map<String, String> backupNow(@Valid @RequestBody BackupRequest request) {
        requireLive();
        Path file = request.destination() == null || request.destination().isBlank() ? backups.backup("manual")
                : backups.backupTo(Path.of(request.destination()));
        return Map.of("file", file.toString());
    }

    /**
     * Exports all data as JSON, as a ZIP of CSV files or as the SQLite database file (RNF-07, IMP-69). The database
     * file, like a backup, is refused while a backup is open.
     *
     * @param format {@code json}, {@code csv} or {@code db}
     * @return the file
     * @throws BusinessRuleException with code {@code backup-open} for the database file while a backup is open
     */
    @GetMapping(value = "/api/data-export", produces = {MediaType.APPLICATION_JSON_VALUE, "application/zip",
            "application/vnd.sqlite3"})
    public ResponseEntity<byte[]> exportData(@RequestParam(defaultValue = "json") String format) {
        ExportFormat chosen = ExportFormat.of(format);
        byte[] body = switch (chosen) {
            case JSON -> export.json();
            case CSV -> export.csvZip();
            case DB -> {
                requireLive();
                yield backups.copy();
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(chosen.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(chosen.fileName(LocalDate.now(clock))).build().toString())
                .body(body);
    }

    private void requireLive() {
        if (!database.state().live()) {
            throw new BusinessRuleException("backup-open", database.state().backup());
        }
    }

    /**
     * Adds {@code backups} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor backupsRootLink() {
        return () -> Link.of("/api/backups", "backups");
    }
}

package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.application.Backups;
import com.planejamentomensal.shared.application.DataExport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.nio.file.Path;
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
 * Backups of the database and export of all data (POR-12, RNF-07).
 *
 * @since 1.0
 */
@RestController
public class BackupController {

    private final Backups backups;
    private final DataExport export;

    /**
     * Creates the controller.
     *
     * @param backups backups
     * @param export  data export
     */
    public BackupController(Backups backups, DataExport export) {
        this.backups = backups;
        this.export = export;
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
     * Makes a backup now, in {@code data/backups/} or where the user chose (POR-12).
     *
     * @param request destination
     * @return {@code {"file": path}}
     */
    @PostMapping("/api/backups")
    public Map<String, String> backupNow(@Valid @RequestBody BackupRequest request) {
        Path file = request.destination() == null || request.destination().isBlank() ? backups.backup("manual")
                : backups.backupTo(Path.of(request.destination()));
        return Map.of("file", file.toString());
    }

    /**
     * Exports all data as JSON or as a ZIP of CSV files (RNF-07).
     *
     * @param format {@code json} or {@code csv}
     * @return the file
     */
    @GetMapping(value = "/api/data-export", produces = {MediaType.APPLICATION_JSON_VALUE, "application/zip"})
    public ResponseEntity<byte[]> exportData(@RequestParam(defaultValue = "json") String format) {
        boolean csv = "csv".equalsIgnoreCase(format);
        return ResponseEntity.ok()
                .contentType(csv ? MediaType.parseMediaType("application/zip") : MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(csv ? "planejamento-mensal-dados.zip" : "planejamento-mensal-dados.json").build().toString())
                .body(csv ? export.csvZip() : export.json());
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

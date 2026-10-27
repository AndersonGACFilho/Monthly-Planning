package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.workbook.ExportedWorkbook;
import com.planejamentomensal.statement.application.workbook.FormulaMode;
import com.planejamentomensal.statement.application.workbook.MonthlyWorkbookExporter;
import com.planejamentomensal.statement.application.workbook.WorkbookTemplate;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Monthly spreadsheet of a billing period and its model (XLS-01, XLS-09, XLS-14, XLS-17).
 *
 * @since 1.0
 */
@RestController
public class WorkbookController {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final MonthlyWorkbookExporter exporter;
    private final WorkbookTemplate template;

    /**
     * Creates the controller.
     *
     * @param exporter spreadsheet exporter
     * @param template spreadsheet model
     */
    public WorkbookController(MonthlyWorkbookExporter exporter, WorkbookTemplate template) {
        this.exporter = exporter;
        this.template = template;
    }

    /**
     * Downloads the monthly spreadsheet (XLS-01, XLS-17).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param mode          formula mode, optional (XLS-09)
     * @param language      {@code pt-BR} or {@code en-US}, optional (XLS-15)
     * @return the XLSX file
     */
    @GetMapping(value = "/api/summaries/{billingPeriod}/workbook", produces = XLSX)
    public ResponseEntity<byte[]> monthlyWorkbook(@PathVariable String billingPeriod,
            @RequestParam(required = false) FormulaMode mode, @RequestParam(required = false) String language) {
        ExportedWorkbook file = exporter.export(BillingPeriod.parse(billingPeriod), mode,
                "en-US".equals(language) || "pt-BR".equals(language) ? language : null);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Formula-Mode", file.mode().name())
                .body(file.content());
    }

    /**
     * Returns the default formula mode and the pending entries, for the export dialog (XLS-07, XLS-10).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param pendingCount  pending entries, as known by the caller from the summary
     * @return options with the download link
     */
    @GetMapping("/api/summaries/{billingPeriod}/workbook-options")
    public WorkbookOptionsModel workbookOptions(@PathVariable String billingPeriod,
            @RequestParam(defaultValue = "0") int pendingCount) {
        WorkbookOptionsModel model = new WorkbookOptionsModel(exporter.defaultMode(), pendingCount);
        model.add(Link.of("/api/summaries/" + BillingPeriod.parse(billingPeriod) + "/workbook", Relations.EXPORT_XLSX));
        return model;
    }

    /**
     * Downloads the spreadsheet model in use (XLS-14).
     *
     * @return the model
     */
    @GetMapping(value = "/api/workbook-model", produces = XLSX)
    public ResponseEntity<byte[]> workbookModel() {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("monthly-workbook-model.xlsx").build().toString())
                .body(template.current());
    }

    /**
     * Replaces the spreadsheet model after checking its named sheets and tables; 409
     * {@code workbook-model-invalid} lists what is missing (XLS-14).
     *
     * @param file new model
     * @return 204
     */
    @PutMapping(value = "/api/workbook-model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replaceWorkbookModel(@RequestPart("file") MultipartFile file) {
        try {
            template.replace(file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * Goes back to the bundled spreadsheet model.
     *
     * @return 204
     */
    @DeleteMapping("/api/workbook-model")
    public ResponseEntity<Void> resetWorkbookModel() {
        template.reset();
        return ResponseEntity.noContent().build();
    }
}

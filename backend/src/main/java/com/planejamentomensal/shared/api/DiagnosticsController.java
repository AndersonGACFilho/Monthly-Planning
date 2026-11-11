package com.planejamentomensal.shared.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.shared.application.DataFolders;
import com.planejamentomensal.shared.application.DebugMode;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Debug mode and interface errors (RNF-11).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private static final Logger LOG = LoggerFactory.getLogger("com.planejamentomensal.interface");

    private final DebugMode debugMode;
    private final DataFolders folders;

    /**
     * Creates the controller.
     *
     * @param debugMode debug mode
     * @param folders   data folders, for the log folder
     */
    public DiagnosticsController(DebugMode debugMode, DataFolders folders) {
        this.debugMode = debugMode;
        this.folders = folders;
    }

    /**
     * Returns the debug mode and the log folder.
     *
     * @return diagnostics
     */
    @GetMapping
    public DiagnosticsModel diagnostics() {
        return new DiagnosticsModel(debugMode.enabled(), folders.logs().toAbsolutePath().toString())
                .add(linkTo(methodOn(DiagnosticsController.class).diagnostics()).withSelfRel())
                .add(linkTo(methodOn(DiagnosticsController.class).reportClientError(null)).withRel(Relations.CLIENT_ERRORS));
    }

    /**
     * Turns the debug mode on or off; the log levels change at once.
     *
     * @param request new mode
     * @return diagnostics after the change
     */
    @PutMapping
    public DiagnosticsModel changeDiagnostics(@Valid @RequestBody DiagnosticsRequest request) {
        debugMode.change(request.debugMode());
        return diagnostics();
    }

    /**
     * Writes an error of the interface to the backend log; line breaks of the single-line fields are dropped so a
     * message cannot fake other log lines.
     *
     * @param error the error
     * @return 204
     */
    @PostMapping("/client-errors")
    public ResponseEntity<Void> reportClientError(@Valid @RequestBody ClientErrorRequest error) {
        LOG.warn("Interface error [{}] at {}: {}{}", oneLine(error.requestId()), oneLine(error.url()),
                oneLine(error.message()), error.detail() == null ? "" : System.lineSeparator() + error.detail());
        return ResponseEntity.noContent().build();
    }

    private static String oneLine(String text) {
        return text == null ? "-" : text.replaceAll("[\\r\\n]+", " ");
    }

    /**
     * Adds {@code diagnostics} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor diagnosticsRootLink() {
        return () -> Link.of("/api/diagnostics", "diagnostics");
    }
}

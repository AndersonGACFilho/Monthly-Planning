package com.planejamentomensal.imports.api;

import com.planejamentomensal.imports.application.ConfirmImportUseCase;
import com.planejamentomensal.imports.application.ImportBatchRepository;
import com.planejamentomensal.imports.application.ImportQueries;
import com.planejamentomensal.imports.application.ManageImportUseCase;
import com.planejamentomensal.imports.application.PreviewImportUseCase;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.InvalidInputException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Imports of Itaú bills: preview, line decisions, confirmation, cancellation and undo (RF-60 to RF-73).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final PreviewImportUseCase preview;
    private final ConfirmImportUseCase confirm;
    private final ManageImportUseCase manage;
    private final ImportQueries queries;
    private final ImportBatchRepository batches;
    private final ImportModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param preview   preview use case
     * @param confirm   confirmation use case
     * @param manage    line edit, cancel and undo use case
     * @param queries   batch queries
     * @param batches   batch storage
     * @param assembler HAL assembler
     */
    public ImportController(PreviewImportUseCase preview, ConfirmImportUseCase confirm, ManageImportUseCase manage,
            ImportQueries queries, ImportBatchRepository batches, ImportModelAssembler assembler) {
        this.preview = preview;
        this.confirm = confirm;
        this.manage = manage;
        this.queries = queries;
        this.batches = batches;
        this.assembler = assembler;
    }

    /**
     * Lists import batches, latest first (RF-72).
     *
     * @param pageable  page, size and sort
     * @param resources paging assembler
     * @return one page of batches
     */
    @GetMapping
    public PagedModel<ImportModel> imports(@ParameterObject @PageableDefault(size = 20, sort = "id",
            direction = Sort.Direction.DESC) Pageable pageable, @Parameter(hidden = true) PagedResourcesAssembler<ImportBatch> resources) {
        return resources.toModel(batches.findAll(pageable), assembler);
    }

    /**
     * Reads a bill and returns the classified preview; nothing is written (RF-60, RF-61, RF-68).
     *
     * @param file          Itaú bill as PDF or XLSX
     * @param cardId        card of the bill
     * @param billingPeriod target billing period, {@code yyyy-MM}
     * @return 201 with the preview and its lines
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportModel> previewImport(@RequestParam("file") MultipartFile file,
            @RequestParam("cardId") long cardId, @RequestParam("billingPeriod") String billingPeriod) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidInputException("invalid-bill-file", file.getOriginalFilename(), e.getMessage());
        }
        ImportBatch batch = preview.preview(cardId, BillingPeriod.parse(billingPeriod), file.getOriginalFilename(), content);
        ImportModel model = assembler.toDetailedModel(batch);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).body(model);
    }

    /**
     * Returns a batch with its lines.
     *
     * @param id batch id
     * @return the batch
     */
    @GetMapping("/{id}")
    public ImportModel importBatch(@PathVariable long id) {
        return assembler.toDetailedModel(queries.get(id));
    }

    /**
     * Changes the decision, member, category or alias of a preview line (RF-69, RF-70).
     *
     * @param id      batch id
     * @param lineId  line id
     * @param request changes
     * @return the batch with its lines
     */
    @PatchMapping("/{id}/lines/{lineId}")
    public ImportModel editImportLine(@PathVariable long id, @PathVariable long lineId,
            @Valid @RequestBody LineEditRequest request) {
        manage.editLine(id, lineId, request.decision(), request.memberId(), request.categoryId(), request.aliasName());
        return assembler.toDetailedModel(queries.get(id));
    }

    /**
     * Confirms the preview and writes the batch (RF-72).
     *
     * @param id      batch id
     * @param request confirmation options
     * @return the confirmed batch
     */
    @PostMapping("/{id}/confirmation")
    public ImportModel confirmImport(@PathVariable long id, @RequestBody(required = false) ConfirmationRequest request) {
        return assembler.toDetailedModel(confirm.confirm(id, request != null && request.acknowledgeTotals()));
    }

    /**
     * Discards a preview.
     *
     * @param id batch id
     * @return the cancelled batch
     */
    @DeleteMapping("/{id}")
    public ImportModel cancelImport(@PathVariable long id) {
        return assembler.toModel(manage.cancel(id));
    }

    /**
     * Undoes the latest confirmed batch of a card (RF-72).
     *
     * @param id batch id
     * @return the undone batch
     */
    @PostMapping("/{id}/reversal")
    public ImportModel undoImport(@PathVariable long id) {
        return assembler.toModel(manage.undo(id));
    }

    /**
     * Adds {@code imports} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor importsRootLink() {
        return () -> Link.of("/api/imports", "imports");
    }
}

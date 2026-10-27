package com.planejamentomensal.imports.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.imports.application.ImportQueries;
import com.planejamentomensal.imports.domain.BatchStatus;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.shared.api.Relations;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the import representation and its state-dependent links: {@code confirm} and {@code cancel} in
 * preview, {@code undo} for the latest confirmed batch of a card (API-09, SPEC section 12 resource table).
 *
 * @since 1.0
 */
@Component
public class ImportModelAssembler implements RepresentationModelAssembler<ImportBatch, ImportModel> {

    private final ImportQueries queries;

    /**
     * Creates the assembler.
     *
     * @param queries batch queries
     */
    public ImportModelAssembler(ImportQueries queries) {
        this.queries = queries;
    }

    /**
     * Converts a batch without its lines, for lists.
     *
     * @param batch batch
     * @return representation with links
     */
    @Override
    public ImportModel toModel(ImportBatch batch) {
        return links(new ImportModel(batch, List.of()), batch);
    }

    /**
     * Converts a batch with its lines.
     *
     * @param batch batch
     * @return representation with lines and links
     */
    public ImportModel toDetailedModel(ImportBatch batch) {
        List<ImportLineModel> lines = queries.lines(batch.id()).stream().map(line -> {
            ImportLineModel model = new ImportLineModel(line);
            if (batch.status() == BatchStatus.PREVIEW) {
                model.add(linkTo(methodOn(ImportController.class).editImportLine(batch.id(), line.id(), null)).withRel("edit"));
            }
            return model;
        }).toList();
        return links(new ImportModel(batch, lines), batch);
    }

    private ImportModel links(ImportModel model, ImportBatch batch) {
        model.add(linkTo(methodOn(ImportController.class).importBatch(batch.id())).withSelfRel());
        model.add(Link.of("/api/cards/" + batch.cardId(), "card"));
        if (batch.status() == BatchStatus.PREVIEW) {
            model.add(linkTo(methodOn(ImportController.class).confirmImport(batch.id(), null)).withRel(Relations.CONFIRM));
            model.add(linkTo(methodOn(ImportController.class).cancelImport(batch.id())).withRel(Relations.CANCEL));
        }
        if (queries.isUndoable(batch)) {
            model.add(linkTo(methodOn(ImportController.class).undoImport(batch.id())).withRel(Relations.UNDO));
        }
        if (batch.status() == BatchStatus.CONFIRMED) {
            model.add(Link.of("/api/review-queue?batch=" + batch.id(), "review-queue"));
        }
        return model;
    }
}

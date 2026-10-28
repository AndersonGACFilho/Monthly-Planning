package com.planejamentomensal.entry.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the entry representation: {@code rename} always, {@code restore-original} only with an alias or
 * override, {@code confirm} only for projected entries, {@code replace-purchase} and {@code delete} only for
 * manual entries no imported bill has seen (SPEC section 12 resource table, API-09).
 *
 * @since 1.0
 */
@Component
public class BillEntryModelAssembler implements RepresentationModelAssembler<EntryRow, BillEntryModel> {

    /**
     * Creates the assembler.
     */
    public BillEntryModelAssembler() {
        // stateless
    }

    /**
     * Converts a row.
     *
     * @param row row
     * @return representation with links
     */
    @Override
    public BillEntryModel toModel(EntryRow row) {
        BillEntryModel model = new BillEntryModel(row);
        model.add(linkTo(methodOn(BillEntryController.class).billEntry(row.entryId())).withSelfRel());
        model.add(linkTo(methodOn(PurchaseController.class).purchase(row.purchaseId())).withRel("purchase"));
        model.add(Link.of("/api/card-bills/" + row.cardBillId(), "bill"));
        model.add(Link.of("/api/cards/" + row.cardId(), "card"));
        if (row.categoryId() != null) {
            model.add(Link.of("/api/categories/" + row.categoryId(), "category"));
        }
        Link alias = linkTo(methodOn(BillEntryController.class).renameEntry(row.entryId(), null)).withRel(Relations.RENAME);
        model.add(alias);
        if (row.renamed()) {
            model.add(alias.withRel(Relations.RESTORE_ORIGINAL));
        }
        if ("PROJECTED".equals(row.status())) {
            model.add(linkTo(methodOn(BillEntryController.class).confirmEntry(row.entryId())).withRel(Relations.CONFIRM));
        }
        if (row.manual()) {
            model.add(linkTo(methodOn(BillEntryController.class).replaceEntryPurchase(row.entryId(), null, null))
                    .withRel(Relations.REPLACE_PURCHASE));
            model.add(linkTo(methodOn(BillEntryController.class).billEntry(row.entryId())).withRel(Relations.DELETE));
        }
        return model;
    }
}

package com.planejamentomensal.entry.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Purchases (the fact behind the installments) with the link to their installments (SPEC section 12).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final EntryQueries queries;
    private final BillEntryModelAssembler entries;

    /**
     * Creates the controller.
     *
     * @param queries entry queries
     * @param entries entry assembler
     */
    public PurchaseController(EntryQueries queries, BillEntryModelAssembler entries) {
        this.queries = queries;
        this.entries = entries;
    }

    /**
     * Summary of a purchase.
     *
     * @param id                  purchase id
     * @param originalDescription description as on the card
     * @param displayDescription  displayed description
     * @param purchaseDate        purchase date
     * @param type                single, installment or subscription
     * @param refundStatus        refund state
     * @param installmentCount    number of installments known
     */
    public record PurchaseModel(long id, String originalDescription, String displayDescription,
            java.time.LocalDate purchaseDate, String type, String refundStatus, int installmentCount) {
    }

    /**
     * Returns a purchase with links to its installments.
     *
     * @param id purchase id
     * @return the purchase
     */
    @GetMapping("/{id}")
    public EntityModel<PurchaseModel> purchase(@PathVariable long id) {
        List<EntryRow> installments = queries.installments(id);
        if (installments.isEmpty()) {
            throw new NotFoundException("purchase", id);
        }
        EntryRow first = installments.getFirst();
        return EntityModel.of(new PurchaseModel(id, first.originalDescription(), first.displayDescription(),
                        first.purchaseDate(), first.type(), first.refundStatus(), installments.size()),
                linkTo(methodOn(PurchaseController.class).purchase(id)).withSelfRel(),
                linkTo(methodOn(PurchaseController.class).installments(id)).withRel(Relations.INSTALLMENTS));
    }

    /**
     * Lists the installments of a purchase.
     *
     * @param id purchase id
     * @return entries in date order
     */
    @GetMapping("/{id}/installments")
    public CollectionModel<BillEntryModel> installments(@PathVariable long id) {
        return entries.toCollectionModel(queries.installments(id));
    }
}

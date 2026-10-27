package com.planejamentomensal.summary.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.summary.application.CardBillSummary;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the card total representation and its state-dependent links: {@code import} while the bill is open,
 * {@code mark-paid} once it is closed and unpaid (IMP-31, API-09).
 *
 * @since 1.0
 */
@Component
public class CardBillModelAssembler implements RepresentationModelAssembler<CardBillSummary, CardBillModel> {

    /**
     * Creates the assembler.
     */
    public CardBillModelAssembler() {
        // stateless
    }

    /**
     * Converts a card total.
     *
     * @param summary card total
     * @return representation with links
     */
    @Override
    public CardBillModel toModel(CardBillSummary summary) {
        CardBillModel model = new CardBillModel(summary);
        String period = summary.bill().billingPeriod().toString();
        model.add(linkTo(methodOn(CardBillController.class).cardBill(summary.cardId(), period)).withSelfRel());
        model.add(Link.of("/api/cards/" + summary.cardId(), "card"));
        model.add(Link.of("/api/bill-entries?billingPeriod=" + period + "&card=" + summary.cardId(), "bill-entries"));
        switch (summary.bill().status()) {
            case "OPEN" -> model.add(Link.of("/api/imports", Relations.IMPORT));
            case "CLOSED" -> model.add(linkTo(methodOn(CardBillController.class).payCardBill(summary.cardId(), period, null))
                    .withRel(Relations.MARK_PAID));
            default -> {
                // paid: nothing left to do
            }
        }
        return model;
    }
}

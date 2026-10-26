package com.planejamentomensal.card.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.domain.Card;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the card representation and its state-dependent links (API-08, API-09, API-11).
 *
 * @since 1.0
 */
@Component
public class CardModelAssembler implements RepresentationModelAssembler<Card, CardModel> {

    private final CardQueries queries;

    /**
     * Creates the assembler.
     *
     * @param queries card queries
     */
    public CardModelAssembler(CardQueries queries) {
        this.queries = queries;
    }

    /**
     * Converts a card.
     *
     * @param card card
     * @return representation with links
     */
    @Override
    public CardModel toModel(Card card) {
        CardModel model = new CardModel(card);
        Link self = linkTo(methodOn(CardController.class).card(card.id())).withSelfRel();
        model.add(self);
        model.add(linkTo(methodOn(CardController.class).cards(null, null)).withRel("cards"));
        model.add(Link.of("/api/members/" + card.defaultMemberId(), "default-member"));
        if (card.isAdditional()) {
            model.add(linkTo(methodOn(CardController.class).card(card.primaryCardId())).withRel("primary-card"));
        } else {
            model.add(linkTo(methodOn(CardController.class).changeCycle(card.id(), null)).withRel("cycles"));
        }
        model.add(self.withRel(card.active() ? Relations.INACTIVATE : Relations.ACTIVATE));
        if (queries.isDeletable(card.id())) {
            model.add(self.withRel(Relations.DELETE));
        }
        return model;
    }
}

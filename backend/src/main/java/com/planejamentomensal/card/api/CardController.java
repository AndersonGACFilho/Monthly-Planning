package com.planejamentomensal.card.api;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardRepository;
import com.planejamentomensal.card.application.DeleteCardUseCase;
import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.card.domain.Card;
import com.planejamentomensal.shared.api.ActivePatch;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credit cards with finals, holder, default member and closing/due cycles (RF-02 to RF-05).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardRepository cards;
    private final CardQueries queries;
    private final SaveCardUseCase save;
    private final DeleteCardUseCase delete;
    private final CardModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param cards     card storage
     * @param queries   card queries
     * @param save      create and edit use case
     * @param delete    delete use case
     * @param assembler HAL assembler
     */
    public CardController(CardRepository cards, CardQueries queries, SaveCardUseCase save, DeleteCardUseCase delete,
            CardModelAssembler assembler) {
        this.cards = cards;
        this.queries = queries;
        this.save = save;
        this.delete = delete;
        this.assembler = assembler;
    }

    /**
     * Lists cards, sorted by nickname by default.
     *
     * @param pageable  page, size and sort
     * @param resources paging assembler
     * @return one page of cards
     */
    @GetMapping
    public PagedModel<CardModel> cards(@PageableDefault(size = 50, sort = "nickname", direction = Sort.Direction.ASC)
            Pageable pageable, PagedResourcesAssembler<Card> resources) {
        return resources.toModel(cards.findAll(pageable), assembler);
    }

    /**
     * Returns one card with its version as {@code ETag}.
     *
     * @param id card id
     * @return the card
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardModel> card(@PathVariable long id) {
        Card card = queries.get(id);
        return ETags.ok(assembler.toModel(card), card.version());
    }

    /**
     * Creates a card with its first cycle.
     *
     * @param request card data
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<CardModel> create(@Valid @RequestBody CardRequest request) {
        Card card = save.create(request.toDetails(), request.closingDay(), request.dueDay());
        CardModel model = assembler.toModel(card);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(card.version())).body(model);
    }

    /**
     * Replaces the data of a card; closing and due day change through {@code cycles}.
     *
     * @param id      card id
     * @param request new data
     * @param ifMatch version being edited
     * @return the updated card
     */
    @PutMapping("/{id}")
    public ResponseEntity<CardModel> replace(@PathVariable long id, @Valid @RequestBody CardRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Card card = save.update(id, request.toDetails(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(card), card.version());
    }

    /**
     * Activates or inactivates a card.
     *
     * @param id      card id
     * @param patch   new active flag
     * @param ifMatch version being edited
     * @return the updated card
     */
    @PatchMapping("/{id}")
    public ResponseEntity<CardModel> patch(@PathVariable long id, @Valid @RequestBody ActivePatch patch,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Card card = save.setActive(id, patch.active(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(card), card.version());
    }

    /**
     * Changes closing and due day from a billing period on, without touching past bills (RF-04).
     *
     * @param id      card id
     * @param request new days and first period
     * @return the updated card
     */
    @PostMapping("/{id}/cycles")
    public ResponseEntity<CardModel> changeCycle(@PathVariable long id, @Valid @RequestBody CycleRequest request) {
        Card card = save.changeCycle(id, request.closingDay(), request.dueDay(), BillingPeriod.parse(request.validFrom()));
        return ETags.ok(assembler.toModel(card), card.version());
    }

    /**
     * Deletes a card without entries or additional cards.
     *
     * @param id card id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable long id) {
        delete.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds {@code cards} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor cardsRootLink() {
        return () -> Link.of("/api/cards", "cards");
    }
}

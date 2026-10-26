package com.planejamentomensal.card.application;

import com.planejamentomensal.card.domain.Card;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to cards for other modules and for the card API.
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class CardQueries {

    private final CardRepository cards;
    private final List<CardReferences> references;

    /**
     * Creates the queries.
     *
     * @param cards      card storage
     * @param references reference checks registered by other modules
     */
    public CardQueries(CardRepository cards, List<CardReferences> references) {
        this.cards = cards;
        this.references = references;
    }

    /**
     * Loads a card.
     *
     * @param id id
     * @return the card
     * @throws NotFoundException if it does not exist
     */
    public Card get(long id) {
        return cards.findById(id).orElseThrow(() -> new NotFoundException("card", id));
    }

    /**
     * Returns the view of a card.
     *
     * @param id id
     * @return the view, or empty
     */
    public Optional<CardView> find(long id) {
        return cards.findById(id).map(CardView::of);
    }

    /**
     * Lists every card, sorted by nickname.
     *
     * @return views of all cards
     */
    public List<CardView> all() {
        return cards.findAll().stream().map(CardView::of).sorted(Comparator.comparing(CardView::nickname)).toList();
    }

    /**
     * Finds the card that owns a final (RF-66).
     *
     * @param lastDigits four digits
     * @return the card, or empty when the final is unknown
     */
    public Optional<CardView> findByFinal(String lastDigits) {
        return cards.findAll().stream().filter(card -> card.lastDigits().contains(lastDigits)).findFirst()
                .map(CardView::of);
    }

    /**
     * Returns the cycle valid in a billing period, following the primary card for additional cards (RF-04,
     * RF-05).
     *
     * @param cardId card id
     * @param period billing period
     * @return the cycle, or empty if none is valid yet
     */
    public Optional<CycleView> cycleFor(long cardId, BillingPeriod period) {
        Card card = get(cardId);
        Card owner = card.isAdditional() ? get(card.primaryCardId()) : card;
        return owner.cycleFor(period).map(c -> new CycleView(c.closingDay(), c.dueDay(), c.validFrom()));
    }

    /**
     * Tells whether a card can be deleted.
     *
     * @param id card id
     * @return {@code true} if no entry or additional card points to it
     */
    public boolean isDeletable(long id) {
        return cards.countByPrimaryCardId(id) == 0 && references.stream().noneMatch(check -> check.isReferenced(id));
    }
}

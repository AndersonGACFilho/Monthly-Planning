package com.planejamentomensal.card.application;

import com.planejamentomensal.card.domain.Card;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a card without entries or additional cards; otherwise it must be inactivated.
 *
 * @since 1.0
 */
@Service
public class DeleteCardUseCase {

    private final CardRepository cards;
    private final CardQueries queries;

    /**
     * Creates the use case.
     *
     * @param cards   card storage
     * @param queries card queries
     */
    public DeleteCardUseCase(CardRepository cards, CardQueries queries) {
        this.cards = cards;
        this.queries = queries;
    }

    /**
     * Deletes the card.
     *
     * @param id card id
     * @throws BusinessRuleException with code {@code card-in-use} if something points to it
     */
    @Transactional
    public void delete(long id) {
        Card card = queries.get(id);
        if (!queries.isDeletable(id)) {
            throw new BusinessRuleException("card-in-use", card.nickname());
        }
        cards.delete(card);
    }
}

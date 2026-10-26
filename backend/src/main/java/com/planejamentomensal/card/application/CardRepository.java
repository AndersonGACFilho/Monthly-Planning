package com.planejamentomensal.card.application;

import com.planejamentomensal.card.domain.Card;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Storage port of cards (SOL-04).
 *
 * @since 1.0
 */
public interface CardRepository {

    /**
     * Finds a card.
     *
     * @param id id
     * @return the card, or empty
     */
    Optional<Card> findById(Long id);

    /**
     * Lists cards.
     *
     * @param pageable page and sort
     * @return one page
     */
    Page<Card> findAll(Pageable pageable);

    /**
     * Lists every card.
     *
     * @return all cards
     */
    List<Card> findAll();

    /**
     * Counts cards whose default member is the given one.
     *
     * @param defaultMemberId member id
     * @return number of cards
     */
    long countByDefaultMemberId(long defaultMemberId);

    /**
     * Counts additional cards of a primary card.
     *
     * @param primaryCardId primary card id
     * @return number of additional cards
     */
    long countByPrimaryCardId(Long primaryCardId);

    /**
     * Stores a card and writes it at once (API-04).
     *
     * @param card card
     * @return the stored card
     */
    Card saveAndFlush(Card card);

    /**
     * Removes a card.
     *
     * @param card card
     */
    void delete(Card card);
}

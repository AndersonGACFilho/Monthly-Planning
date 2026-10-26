package com.planejamentomensal.card.application;

/**
 * Tells whether something in another module points to a card, such as bill entries (SOL-02).
 *
 * @since 1.0
 */
public interface CardReferences {

    /**
     * Tells whether the card is used.
     *
     * @param cardId card id
     * @return {@code true} if deleting it would lose history
     */
    boolean isReferenced(long cardId);
}

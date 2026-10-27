package com.planejamentomensal.imports.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.InvalidInputException;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Maps the holder (PDF section title, XLSX "Nome") to the default member of the matching card, and each
 * final to its registered card; an unknown final goes to the review queue (RF-66, RF-02, RF-05).
 *
 * @since 1.0
 */
@Service
public class CardResolver {

    private final CardQueries cards;

    /**
     * Creates the resolver.
     *
     * @param cards card queries
     */
    public CardResolver(CardQueries cards) {
        this.cards = cards;
    }

    /**
     * Cards that share one bill: the primary card and its additional cards.
     *
     * @param billingCard primary card
     * @param cards       every card of the group
     */
    public record CardGroup(CardView billingCard, List<CardView> cards) {

        /**
         * Tells whether a final belongs to the group.
         *
         * @param lastDigits final
         * @return {@code true} if registered on one of the cards
         */
        public boolean hasFinal(String lastDigits) {
            return cards.stream().anyMatch(c -> c.lastDigits().contains(lastDigits));
        }
    }

    /**
     * Member and final status of one line.
     *
     * @param memberId     default member of the matching card
     * @param unknownFinal {@code true} if the final is not registered
     */
    public record Resolution(long memberId, boolean unknownFinal) {
    }

    /**
     * Loads the group of the chosen card; an additional card resolves to its primary card (RF-05).
     *
     * @param cardId card chosen in the import screen
     * @return the group
     * @throws InvalidInputException with code {@code import-card-unknown} if the card does not exist
     */
    public CardGroup group(long cardId) {
        CardView chosen = cards.find(cardId).orElseThrow(() -> new InvalidInputException("import-card-unknown", cardId));
        CardView billing = cards.find(chosen.billingCardId()).orElseThrow();
        List<CardView> members = cards.all().stream().filter(c -> c.billingCardId() == billing.id()).toList();
        return new CardGroup(billing, members);
    }

    /**
     * Resolves one line.
     *
     * @param group      card group
     * @param lastDigits final on the line, or {@code null} (PDF)
     * @param holderName holder on the line, or {@code null}
     * @return member and final status
     */
    public Resolution resolve(CardGroup group, String lastDigits, String holderName) {
        if (lastDigits != null) {
            return group.cards().stream().filter(c -> c.lastDigits().contains(lastDigits)).findFirst()
                    .map(c -> new Resolution(c.defaultMemberId(), false))
                    .orElse(new Resolution(group.billingCard().defaultMemberId(), true));
        }
        if (holderName != null) {
            String holder = DescriptionNormalizer.normalize(holderName);
            return group.cards().stream()
                    .filter(c -> DescriptionNormalizer.normalize(c.holderName()).equals(holder))
                    .findFirst()
                    .map(c -> new Resolution(c.defaultMemberId(), false))
                    .orElse(new Resolution(group.billingCard().defaultMemberId(), false));
        }
        return new Resolution(group.billingCard().defaultMemberId(), false);
    }
}

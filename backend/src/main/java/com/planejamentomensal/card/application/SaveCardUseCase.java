package com.planejamentomensal.card.application;

import com.planejamentomensal.card.domain.Card;
import com.planejamentomensal.card.domain.CardCycle;
import com.planejamentomensal.card.domain.CardDetails;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Versions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and edits cards and their cycles (RF-02 to RF-05).
 *
 * @since 1.0
 */
@Service
public class SaveCardUseCase {

    /** Validity of the first cycle: it covers every period before later changes. */
    public static final BillingPeriod FIRST_CYCLE_START = BillingPeriod.of(2000, 1);

    private final CardRepository cards;
    private final CardQueries queries;
    private final MemberQueries members;

    /**
     * Creates the use case.
     *
     * @param cards   card storage
     * @param queries card queries
     * @param members member queries, to check the default member
     */
    public SaveCardUseCase(CardRepository cards, CardQueries queries, MemberQueries members) {
        this.cards = cards;
        this.queries = queries;
        this.members = members;
    }

    /**
     * Creates a card.
     *
     * @param details    card data
     * @param closingDay closing day of the first cycle (ignored for additional cards)
     * @param dueDay     due day of the first cycle (ignored for additional cards)
     * @return the stored card
     * @throws BusinessRuleException if the member or primary card is invalid or a final is taken
     */
    @Transactional
    public Card create(CardDetails details, int closingDay, int dueDay) {
        validate(details, null);
        return cards.saveAndFlush(new Card(details, new CardCycle(closingDay, dueDay, FIRST_CYCLE_START)));
    }

    /**
     * Replaces the data of a card; cycles are changed with {@link #changeCycle}.
     *
     * @param id              card id
     * @param details         new data
     * @param expectedVersion version the client edited, or {@code null}
     * @return the updated card
     */
    @Transactional
    public Card update(long id, CardDetails details, Long expectedVersion) {
        Card card = queries.get(id);
        Versions.check(expectedVersion, card.version());
        validate(details, id);
        card.update(details);
        return cards.saveAndFlush(card);
    }

    /**
     * Activates or inactivates a card.
     *
     * @param id              card id
     * @param active          new state
     * @param expectedVersion version the client edited, or {@code null}
     * @return the updated card
     */
    @Transactional
    public Card setActive(long id, boolean active, Long expectedVersion) {
        Card card = queries.get(id);
        Versions.check(expectedVersion, card.version());
        card.setActive(active);
        return cards.saveAndFlush(card);
    }

    /**
     * Changes closing and due day from a billing period on, keeping past periods (RF-04).
     *
     * @param id         card id
     * @param closingDay new closing day
     * @param dueDay     new due day
     * @param validFrom  first period with the new days
     * @return the updated card
     * @throws BusinessRuleException with code {@code additional-card-cycle} for additional cards
     */
    @Transactional
    public Card changeCycle(long id, int closingDay, int dueDay, BillingPeriod validFrom) {
        Card card = queries.get(id);
        if (card.isAdditional()) {
            throw new BusinessRuleException("additional-card-cycle", card.nickname());
        }
        card.changeCycle(new CardCycle(closingDay, dueDay, validFrom));
        return cards.saveAndFlush(card);
    }

    private void validate(CardDetails details, Long ownId) {
        if (members.find(details.defaultMemberId()).isEmpty()) {
            throw new BusinessRuleException("card-member-unknown", details.defaultMemberId());
        }
        if (details.primaryCardId() != null) {
            Card primary = cards.findById(details.primaryCardId())
                    .orElseThrow(() -> new BusinessRuleException("card-primary-invalid", details.primaryCardId()));
            if (primary.isAdditional() || primary.id().equals(ownId)) {
                throw new BusinessRuleException("card-primary-invalid", details.primaryCardId());
            }
        }
        for (String lastDigits : details.lastDigits()) {
            cards.findAll().stream()
                    .filter(other -> !other.id().equals(ownId) && other.lastDigits().contains(lastDigits))
                    .findFirst()
                    .ifPresent(other -> {
                        throw new BusinessRuleException("card-final-taken", lastDigits, other.nickname());
                    });
        }
    }
}

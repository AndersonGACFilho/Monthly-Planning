package com.planejamentomensal.card.api;

import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.Card;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a card.
 *
 * @since 1.0
 */
@Relation(itemRelation = "card", collectionRelation = "cards")
public class CardModel extends RepresentationModel<CardModel> {

    /**
     * Cycle of a card, valid from a period.
     *
     * @param closingDay closing day
     * @param dueDay     due day
     * @param validFrom  first billing period, {@code yyyy-MM}
     */
    public record Cycle(int closingDay, int dueDay, String validFrom) {
    }

    /** Id. */
    public final long id;
    /** Nickname. */
    public final String nickname;
    /** Issuer. */
    public final Bank bank;
    /** Finals. */
    public final List<String> lastDigits;
    /** Holder name. */
    public final String holderName;
    /** Primary card of an additional card. */
    public final Long primaryCardId;
    /** Default member. */
    public final long defaultMemberId;
    /** Active flag. */
    public final boolean active;
    /** Cycles, oldest first; empty for additional cards. */
    public final List<Cycle> cycles;

    /**
     * Copies a card.
     *
     * @param card card
     */
    public CardModel(Card card) {
        this.id = card.id();
        this.nickname = card.nickname();
        this.bank = card.bank();
        this.lastDigits = card.lastDigits();
        this.holderName = card.holderName();
        this.primaryCardId = card.primaryCardId();
        this.defaultMemberId = card.defaultMemberId();
        this.active = card.active();
        this.cycles = card.cycles().stream()
                .map(c -> new Cycle(c.closingDay(), c.dueDay(), c.validFrom().toString()))
                .toList();
    }
}

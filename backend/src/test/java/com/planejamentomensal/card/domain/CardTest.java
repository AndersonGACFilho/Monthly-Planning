package com.planejamentomensal.card.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.BillingPeriod;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardTest {

    private static CardDetails details(Long primary, String... finals) {
        return new CardDetails("Itaú Platinum", Bank.ITAU, List.of(finals), "MARIANA S F SILVA", primary, 1L);
    }

    @Test
    @DisplayName("RF-03: a card keeps several 4-digit finals")
    void keepsFinals() {
        Card card = new Card(details(null, "4102", "4090", "4102"), new CardCycle(3, 10, BillingPeriod.of(2000, 1)));
        assertThat(card.lastDigits()).containsExactly("4102", "4090");
        assertThatThrownBy(() -> new Card(details(null, "41"), new CardCycle(3, 10, BillingPeriod.of(2000, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CardCycle(0, 10, BillingPeriod.of(2000, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-04: a new cycle applies from its period on and past periods keep the old one")
    void cyclesByPeriod() {
        Card card = new Card(details(null, "4102"), new CardCycle(3, 10, BillingPeriod.of(2000, 1)));
        card.changeCycle(new CardCycle(28, 5, BillingPeriod.of(2027, 1)));
        assertThat(card.cycleFor(BillingPeriod.of(2026, 12)).orElseThrow().dueDay()).isEqualTo(10);
        assertThat(card.cycleFor(BillingPeriod.of(2027, 1)).orElseThrow().dueDay()).isEqualTo(5);
        assertThat(card.cycleFor(BillingPeriod.of(2027, 6)).orElseThrow().closingDay()).isEqualTo(28);
        card.changeCycle(new CardCycle(27, 4, BillingPeriod.of(2027, 1)));
        assertThat(card.cycles()).hasSize(2);
        assertThat(card.cycleFor(BillingPeriod.of(1999, 1))).isEmpty();
    }

    @Test
    @DisplayName("RF-05: additional cards have no cycle of their own")
    void additionalCardsFollowPrimary() {
        Card additional = new Card(details(7L, "9103"), new CardCycle(3, 10, BillingPeriod.of(2000, 1)));
        assertThat(additional.isAdditional()).isTrue();
        assertThat(additional.cycles()).isEmpty();
        assertThatThrownBy(() -> additional.changeCycle(new CardCycle(3, 10, BillingPeriod.of(2027, 1))))
                .isInstanceOf(IllegalStateException.class);
    }
}

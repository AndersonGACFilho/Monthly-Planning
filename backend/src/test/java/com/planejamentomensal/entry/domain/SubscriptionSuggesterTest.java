package com.planejamentomensal.entry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.planejamentomensal.shared.domain.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionSuggesterTest {

    private static SubscriptionSuggester.PreviousCharge charge(String description, String amount, boolean single) {
        return new SubscriptionSuggester.PreviousCharge(description, Money.of(amount), single);
    }

    @Test
    @DisplayName("RN-23: same store without installments and similar amount in the previous period suggests a subscription")
    void suggests() {
        assertThat(SubscriptionSuggester.looksLikeSubscription("EC *PARAMOUNTPLUSOS", Money.of("24.43"),
                List.of(charge("EC *PARAMOUNTPLUSOS", "24.43", true)))).isTrue();
        assertThat(SubscriptionSuggester.looksLikeSubscription("Amazon Kindle UnltdSAO", Money.of("26.00"),
                List.of(charge("Amazon Kindle UnltdSAO", "24.90", true)))).isTrue();
    }

    @Test
    @DisplayName("RN-23: different amount, store or installment purchases do not suggest a subscription")
    void doesNotSuggest() {
        assertThat(SubscriptionSuggester.looksLikeSubscription("POSTO AGRONOMIA", Money.of("80.00"),
                List.of(charge("POSTO AGRONOMIA", "50.00", true)))).isFalse();
        assertThat(SubscriptionSuggester.looksLikeSubscription("POSTO AGRONOMIA", Money.of("50.00"),
                List.of(charge("POSTO AGRONOMIA", "50.00", false)))).isFalse();
        assertThat(SubscriptionSuggester.looksLikeSubscription("CINEMA", Money.of("30.00"),
                List.of(charge("TEATRO", "30.00", true)))).isFalse();
        assertThat(SubscriptionSuggester.looksLikeSubscription("ZERO", Money.ZERO, List.of(charge("ZERO", "0.00", true)))).isTrue();
    }
}

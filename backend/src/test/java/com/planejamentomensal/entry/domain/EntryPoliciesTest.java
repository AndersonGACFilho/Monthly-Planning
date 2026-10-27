package com.planejamentomensal.entry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntryPoliciesTest {

    private static final LocalDate DAY = LocalDate.of(2026, 9, 3);

    @Test
    @DisplayName("RN-10, RN-11: installment key adds the installment number to the purchase key")
    void keys() {
        String a = EntryKeyFactory.purchaseKey(1, DAY, "PETLOVE SAUD*Petlove SS", Money.of("12.00"), 1, 1);
        String b = EntryKeyFactory.purchaseKey(1, DAY, "petlove saud petlove ss", Money.of("12.00"), 1, 1);
        assertThat(a).isEqualTo(b);
        assertThat(EntryKeyFactory.installmentKey(a, 3, null)).isEqualTo(a + "|3");
        assertThat(EntryKeyFactory.installmentKey(a, null, "2026-11")).isEqualTo(a + "|-|2026-11");
        assertThat(EntryKeyFactory.purchaseKey(1, DAY, "X", Money.of("48.01"), 2, 1)).contains("|9602|");
    }

    @Test
    @DisplayName("RN-12, RN-25: identical purchases on the same day differ by occurrence")
    void occurrence() {
        String first = EntryKeyFactory.purchaseKey(1, DAY, "PETLOVE", Money.of("12.00"), 1, 1);
        String second = EntryKeyFactory.purchaseKey(1, DAY, "PETLOVE", Money.of("12.00"), 1, 2);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("RN-04: installment n/N projects n+1 … N in the following periods")
    void projectsRemaining() {
        var projections = InstallmentProjector.remainingInstallments(3, 5, BillingPeriod.of(2026, 11));
        assertThat(projections).containsExactly(
                new InstallmentProjector.Projection(4, BillingPeriod.of(2026, 12)),
                new InstallmentProjector.Projection(5, BillingPeriod.of(2027, 1)));
        assertThat(InstallmentProjector.remainingInstallments(5, 5, BillingPeriod.of(2026, 11))).isEmpty();
    }

    @Test
    @DisplayName("RN-09: subscriptions are projected up to the horizon")
    void projectsSubscription() {
        var charges = InstallmentProjector.subscriptionCharges(BillingPeriod.of(2026, 11), BillingPeriod.of(2027, 2));
        assertThat(charges).extracting(InstallmentProjector.Projection::period)
                .containsExactly(BillingPeriod.of(2026, 12), BillingPeriod.of(2027, 1), BillingPeriod.of(2027, 2));
    }

    @Test
    @DisplayName("RF-26: split by percentage and by amount never loses cents")
    void splits() {
        List<PurchaseSplit> half = List.of(new PurchaseSplit(1, new BigDecimal("50"), null),
                new PurchaseSplit(2, new BigDecimal("50"), null));
        assertThat(SplitPolicy.divide(Money.of("100.01"), half)).extracting(SplitPolicy.Share::amount)
                .containsExactly(Money.of("50.01"), Money.of("50.00"));
        List<PurchaseSplit> mixed = List.of(new PurchaseSplit(1, null, Money.of("30.00")),
                new PurchaseSplit(2, new BigDecimal("100"), null));
        assertThat(SplitPolicy.divide(Money.of("100.00"), mixed)).extracting(SplitPolicy.Share::amount)
                .containsExactly(Money.of("30.00"), Money.of("70.00"));
        List<PurchaseSplit> amounts = List.of(new PurchaseSplit(1, null, Money.of("30.00")),
                new PurchaseSplit(2, null, Money.of("60.00")));
        assertThat(SplitPolicy.divide(Money.of("100.00"), amounts)).extracting(SplitPolicy.Share::amount)
                .containsExactly(Money.of("30.00"), Money.of("70.00"));
        assertThat(SplitPolicy.divide(Money.of("-10.00"), List.of(new PurchaseSplit(3, new BigDecimal("100"), null))))
                .extracting(SplitPolicy.Share::amount).containsExactly(Money.of("-10.00"));
        assertThatThrownBy(() -> SplitPolicy.divide(Money.of("1"), List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PurchaseSplit(1, BigDecimal.ONE, Money.of("1"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-76: a split is complete when it covers 100% of the amount")
    void completeSplits() {
        Money amount = Money.of("90.00");
        assertThat(SplitPolicy.isComplete(amount, List.of(new PurchaseSplit(1, new BigDecimal("100"), null)))).isTrue();
        assertThat(SplitPolicy.isComplete(amount, List.of(new PurchaseSplit(1, new BigDecimal("60"), null)))).isFalse();
        assertThat(SplitPolicy.isComplete(amount, List.of(new PurchaseSplit(1, null, Money.of("90.00"))))).isTrue();
        assertThat(SplitPolicy.isComplete(amount, List.of(new PurchaseSplit(1, null, Money.of("50.00"))))).isFalse();
        assertThat(SplitPolicy.isComplete(amount, List.of(new PurchaseSplit(1, null, Money.of("50.00")),
                new PurchaseSplit(2, new BigDecimal("100"), null)))).isTrue();
        assertThat(SplitPolicy.isComplete(amount, List.of())).isFalse();
    }

    @Test
    @DisplayName("RN-26, RN-27, RN-28: a negative line refunds the most fitting earlier purchase of the same store")
    void refunds() {
        var shoes = new RefundMatcher.Candidate(1, 7, LocalDate.of(2026, 9, 1), "KAZZU AZZEE CALCADOS", Money.of("200.00"));
        var shoesExact = new RefundMatcher.Candidate(2, 7, LocalDate.of(2026, 8, 1), "Kazzu Azzee Calcados", Money.of("80.00"));
        var other = new RefundMatcher.Candidate(3, 7, LocalDate.of(2026, 9, 2), "POSTO AGRONOMIA", Money.of("80.00"));
        var list = List.of(shoes, shoesExact, other);
        assertThat(RefundMatcher.find(7, LocalDate.of(2026, 9, 10), "KAZZU AZZEE CALCADOS", Money.of("-80.00"), list))
                .contains(shoesExact);
        assertThat(RefundMatcher.find(7, LocalDate.of(2026, 9, 10), "KAZZU AZZEE CALCADOS", Money.of("-50.00"), list))
                .contains(shoes);
        assertThat(RefundMatcher.find(7, LocalDate.of(2026, 9, 10), "Redução Mensalidade - P", Money.of("-33.25"), list))
                .isEmpty();
        assertThat(RefundMatcher.find(8, LocalDate.of(2026, 9, 10), "KAZZU AZZEE CALCADOS", Money.of("-80.00"), list))
                .isEmpty();
        assertThat(RefundMatcher.find(7, LocalDate.of(2026, 7, 1), "KAZZU AZZEE CALCADOS", Money.of("-80.00"), list))
                .isEmpty();
    }
}

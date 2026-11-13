package com.planejamentomensal.statement.application;

import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.recurrence.application.RecurrenceForecast;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.statement.domain.MemberStatement.FutureLine;
import com.planejamentomensal.statement.domain.MemberStatement.FuturePeriod;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Future expenses of a member for the statement (RF-82, IMP-64): for each of the next periods, the member share of the
 * projected installments and subscriptions of every card, followed by the member's fixed expenses paid outside the
 * card. Card-paid fixed expenses are left out, since their card entry already counts them (RF-33).
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class FutureExpensesBuilder {

    /** Periods shown after the statement period; the same reach as the overview's committed installments (RF-17). */
    static final int PERIODS_AHEAD = 3;

    private static final List<String> PROJECTED_TYPES = List.of("INSTALLMENT", "SUBSCRIPTION");

    private final EntryQueries entries;
    private final RecurrenceForecast recurrence;

    /**
     * Creates the builder.
     *
     * @param entries    card entries
     * @param recurrence fixed expenses of future periods
     */
    public FutureExpensesBuilder(EntryQueries entries, RecurrenceForecast recurrence) {
        this.entries = entries;
        this.recurrence = recurrence;
    }

    /**
     * Builds the future expenses of a member; periods without any expense are left out.
     *
     * @param memberId member
     * @param period   statement period
     * @param cards    every card by id
     * @param useAlias show aliases instead of original descriptions
     * @return future periods in order
     */
    public List<FuturePeriod> build(long memberId, BillingPeriod period, Collection<CardView> cards, boolean useAlias) {
        Map<BillingPeriod, List<FutureLine>> byPeriod = new TreeMap<>();
        BillingPeriod last = period.plusMonths(PERIODS_AHEAD);
        cards.stream().filter(card -> card.primaryCardId() == null)
                .forEach(card -> projectedEntries(card.id(), period, last).forEach(row -> {
                    Money share = row.memberAmounts(card.defaultMemberId()).get(memberId);
                    if (share != null) {
                        String description = useAlias ? row.displayDescription() : row.originalDescription();
                        lines(byPeriod, row.billingPeriod()).add(new FutureLine(description, row.installment(), share));
                    }
                }));
        for (int i = 1; i <= PERIODS_AHEAD; i++) {
            BillingPeriod future = period.plusMonths(i);
            recurrence.fixedExpenses(future).stream()
                    .filter(o -> o.memberId() == memberId && o.paymentMethod() != PaymentMethod.CARD)
                    .map(o -> new FutureLine(o.description(), null, o.amount()))
                    .forEach(line -> lines(byPeriod, future).add(line));
        }
        return byPeriod.entrySet().stream()
                .map(e -> new FuturePeriod(e.getKey(), e.getValue(),
                        e.getValue().stream().map(FutureLine::amount).reduce(Money.ZERO, Money::plus)))
                .toList();
    }

    private List<EntryRow> projectedEntries(long cardId, BillingPeriod period, BillingPeriod last) {
        return entries.list(new EntryFilter(null, cardId, null, null, null, null, "PROJECTED")).stream()
                .filter(row -> row.billingPeriod().compareTo(period) > 0 && row.billingPeriod().compareTo(last) <= 0)
                .filter(row -> PROJECTED_TYPES.contains(row.type()))
                .toList();
    }

    private static List<FutureLine> lines(Map<BillingPeriod, List<FutureLine>> byPeriod, BillingPeriod period) {
        return byPeriod.computeIfAbsent(period, k -> new ArrayList<>());
    }
}

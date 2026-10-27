package com.planejamentomensal.summary.application;

import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.entry.application.CardBillQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CardBillBreakdown;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator.FutureInstallment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Card totals of a period: one per active primary card and per card with entries (RF-14, RF-36, RF-37).
 *
 * @since 1.0
 */
@Service
@Transactional
public class CardBillSummaries {

    private final PeriodData data;
    private final CardBillQueries bills;

    /**
     * Creates the service.
     *
     * @param data  period data
     * @param bills card bills
     */
    public CardBillSummaries(PeriodData data, CardBillQueries bills) {
        this.data = data;
        this.bills = bills;
    }

    /**
     * Returns the card totals of a period.
     *
     * @param period billing period
     * @return one summary per card, by nickname
     */
    public List<CardBillSummary> forPeriod(BillingPeriod period) {
        return build(period, data.collect(period).entries(), data.futureInstallments(period), r -> true);
    }

    /**
     * Builds card totals from entries already collected; {@code include} keeps only some entries (a member filter).
     *
     * @param period  billing period
     * @param rows    entries of the period
     * @param future  projected installments of later periods
     * @param include which entries count
     * @return one summary per card, by nickname
     */
    List<CardBillSummary> build(BillingPeriod period, List<EntryRow> rows, List<EntryRow> future, Predicate<EntryRow> include) {
        Map<Long, Money> committed = CommittedInstallmentsCalculator.byCard(period, future.stream().filter(include)
                .map(r -> new FutureInstallment(r.billingPeriod(), r.cardId(), r.amount())).toList());
        Set<Long> cardIds = new LinkedHashSet<>();
        List<CardView> primary = data.primaryCards();
        primary.stream().filter(CardView::active).forEach(c -> cardIds.add(c.id()));
        rows.forEach(r -> cardIds.add(r.cardId()));
        List<CardBillSummary> result = new ArrayList<>();
        for (long cardId : cardIds) {
            CardView card = primary.stream().filter(c -> c.id() == cardId).findFirst().orElse(null);
            CardBillBreakdown breakdown = CardBillBreakdown.EMPTY;
            int unconfirmed = 0;
            int pending = 0;
            for (EntryRow row : rows) {
                if (row.cardId() != cardId || !include.test(row)) {
                    continue;
                }
                if (PeriodData.UNCONFIRMED.equals(row.status())) {
                    unconfirmed++;
                    continue;
                }
                breakdown = breakdown.plus(row.type(), row.amount());
                if ("PENDING_REVIEW".equals(row.reviewStatus())) {
                    pending++;
                }
            }
            result.add(new CardBillSummary(cardId, card == null ? String.valueOf(cardId) : card.nickname(),
                    card == null ? List.of() : card.lastDigits(), bills.bill(cardId, period), breakdown,
                    committed.getOrDefault(cardId, Money.ZERO), unconfirmed, pending));
        }
        result.sort(Comparator.comparing(CardBillSummary::nickname, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}

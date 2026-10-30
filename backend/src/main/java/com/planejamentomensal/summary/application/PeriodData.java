package com.planejamentomensal.summary.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.carryover.application.CarryOverCommands;
import com.planejamentomensal.carryover.application.CarryOverView;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.recurrence.application.OccurrenceView;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.summary.domain.CarryOvers;
import com.planejamentomensal.summary.domain.ExpenseLine;
import com.planejamentomensal.summary.domain.ExpenseSource;
import com.planejamentomensal.summary.domain.IncomeLine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Collects the incomes, the expense lines and the carry-overs of a period from the other modules (RF-11, RF-26,
 * RF-28, RF-33, Q6).
 *
 * @since 1.0
 */
@Component
public class PeriodData {

    /** Entry status left out of every total (Q6). */
    static final String UNCONFIRMED = "UNCONFIRMED";

    private final RecurrenceCommands recurrence;
    private final VariableExpenseCommands expenses;
    private final EntryQueries entries;
    private final CardQueries cards;
    private final CarryOverCommands carryOvers;

    /**
     * Creates the collector.
     *
     * @param recurrence incomes and fixed expenses
     * @param expenses   variable expenses
     * @param entries    card entries
     * @param cards      card queries
     * @param carryOvers carry-overs
     */
    public PeriodData(RecurrenceCommands recurrence, VariableExpenseCommands expenses, EntryQueries entries,
            CardQueries cards, CarryOverCommands carryOvers) {
        this.recurrence = recurrence;
        this.expenses = expenses;
        this.entries = entries;
        this.cards = cards;
        this.carryOvers = carryOvers;
    }

    /**
     * Incomes and expense lines of one period.
     *
     * @param incomes  income lines
     * @param expenses expense lines
     * @param entries  card entries of the period, UNCONFIRMED included
     * @param carries  amounts carried in and out of the period
     */
    public record Lines(List<IncomeLine> incomes, List<ExpenseLine> expenses, List<EntryRow> entries, CarryOvers carries) {
    }

    /**
     * Collects a period. Card-paid fixed expenses are left out: the bill holds them (RF-33).
     *
     * @param period billing period
     * @return lines
     */
    public Lines collect(BillingPeriod period) {
        List<IncomeLine> incomes = new ArrayList<>();
        List<ExpenseLine> lines = new ArrayList<>();
        for (OccurrenceView o : recurrence.views(period)) {
            if (o.income()) {
                incomes.add(new IncomeLine(o.memberId(), o.amount()));
            } else if (o.paymentMethod() != PaymentMethod.CARD) {
                lines.add(new ExpenseLine(ExpenseSource.FIXED, o.memberId(), o.categoryId(), o.amount(), null, o.memberId()));
            }
        }
        expenses.views(period).forEach(e -> lines.add(
                new ExpenseLine(ExpenseSource.VARIABLE, e.memberId(), e.categoryId(), e.amount(), null, e.memberId())));
        List<EntryRow> rows = entries.list(new EntryFilter(period, null, null, null, null, null, null));
        Map<Long, Long> payers = new HashMap<>();
        for (EntryRow row : rows) {
            if (UNCONFIRMED.equals(row.status())) {
                continue;
            }
            long payer = payers.computeIfAbsent(row.cardId(), this::payerOf);
            row.memberAmounts(payer).forEach((member, amount) -> lines.add(
                    new ExpenseLine(ExpenseSource.CARD, member, row.categoryId(), amount, row.cardId(), payer)));
        }
        return new Lines(incomes, lines, rows,
                new CarryOvers(byMember(carryOvers.incoming(period)), byMember(carryOvers.outgoing(period))));
    }

    private static Map<Long, Money> byMember(List<CarryOverView> carries) {
        return carries.stream().collect(Collectors.toMap(CarryOverView::memberId, CarryOverView::amount, Money::plus));
    }

    /**
     * Projected installments of every later period (RF-14, RF-17).
     *
     * @param period current period
     * @return projected installment entries after the period
     */
    public List<EntryRow> futureInstallments(BillingPeriod period) {
        return cards.all().stream().filter(c -> c.primaryCardId() == null)
                .flatMap(c -> entries.list(new EntryFilter(null, c.id(), null, null, null, null, "PROJECTED")).stream())
                .filter(r -> r.billingPeriod().compareTo(period) > 0 && "INSTALLMENT".equals(r.type()))
                .toList();
    }

    /**
     * Returns the active primary cards.
     *
     * @return cards
     */
    public List<CardView> primaryCards() {
        return cards.all().stream().filter(c -> c.primaryCardId() == null).toList();
    }

    /**
     * Payer of a card: its default member (Q8).
     *
     * @param cardId primary card
     * @return member id
     */
    long payerOf(long cardId) {
        return cards.find(cardId).map(CardView::defaultMemberId).orElse(0L);
    }
}

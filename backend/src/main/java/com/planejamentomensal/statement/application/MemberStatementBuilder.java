package com.planejamentomensal.statement.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.carryover.application.CarryOverCommands;
import com.planejamentomensal.carryover.application.CarryOverView;
import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.entry.application.CardBillQueries;
import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.expense.application.ExpenseView;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.recurrence.application.OccurrenceView;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.MemberStatement.CardGroup;
import com.planejamentomensal.statement.domain.MemberStatement.CardLine;
import com.planejamentomensal.statement.domain.MemberStatement.Carry;
import com.planejamentomensal.statement.domain.MemberStatement.CommittedPeriod;
import com.planejamentomensal.statement.domain.MemberStatement.ExpenseLine;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the statement of one member in one billing period: the member's share of each card entry, their fixed
 * and variable expenses, the installments committed ahead and the payment data (RF-81 to RF-83).
 *
 * <p>Card entries follow the overview rules: UNCONFIRMED entries are left out (Q6); an entry without shares
 * belongs to the card's default member. The PIX key shown is the one of the card payer when the member is a third
 * party, otherwise the member's own. What the member still owed from the previous period adds to the total due; what
 * they carried into the next period is shown apart, so the period keeps its own total (RF-28).</p>
 *
 * @since 1.0
 */
@Service
@Transactional
public class MemberStatementBuilder {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MemberQueries members;
    private final CardQueries cards;
    private final CardBillQueries bills;
    private final CategoryQueries categories;
    private final EntryQueries entries;
    private final RecurrenceCommands recurrence;
    private final VariableExpenseCommands expenses;
    private final CarryOverCommands carryOvers;
    private final Clock clock;

    /**
     * Creates the builder.
     *
     * @param members    member queries
     * @param cards      card queries
     * @param bills      card bills
     * @param categories category queries
     * @param entries    card entries
     * @param recurrence incomes and fixed expenses
     * @param expenses   variable expenses
     * @param carryOvers carry-overs
     * @param clock      clock for the issue date
     */
    public MemberStatementBuilder(MemberQueries members, CardQueries cards, CardBillQueries bills,
            CategoryQueries categories, EntryQueries entries, RecurrenceCommands recurrence,
            VariableExpenseCommands expenses, CarryOverCommands carryOvers, Clock clock) {
        this.members = members;
        this.cards = cards;
        this.bills = bills;
        this.categories = categories;
        this.entries = entries;
        this.recurrence = recurrence;
        this.expenses = expenses;
        this.carryOvers = carryOvers;
        this.clock = clock;
    }

    /**
     * Builds one statement.
     *
     * @param memberId member
     * @param period   billing period
     * @param options  export options
     * @return the statement
     * @throws NotFoundException if the member does not exist
     */
    public MemberStatement build(long memberId, BillingPeriod period, StatementOptions options) {
        MemberView member = members.find(memberId).orElseThrow(() -> new NotFoundException("member", memberId));
        Map<Long, CardView> cardById = new HashMap<>();
        cards.all().forEach(c -> cardById.put(c.id(), c));
        Map<Long, String> categoryNames = new HashMap<>();
        categories.all().forEach(c -> categoryNames.put(c.id(), c.name()));

        Map<Long, List<CardLine>> linesByCard = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();
        Long payer = null;
        for (EntryRow row : entries.list(new EntryFilter(period, null, null, null, null, null, null))) {
            if ("UNCONFIRMED".equals(row.status())) {
                continue;
            }
            long cardPayer = payerOf(cardById, row.cardId());
            Money share = row.memberAmounts(cardPayer).get(memberId);
            if (share == null) {
                continue;
            }
            payer = payer == null ? cardPayer : payer;
            boolean isPending = "PENDING_REVIEW".equals(row.reviewStatus());
            String description = options.useAlias() ? row.displayDescription() : row.originalDescription();
            if (isPending) {
                pending.add(description);
            }
            linesByCard.computeIfAbsent(row.cardId(), k -> new ArrayList<>()).add(new CardLine(row.entryDate(), description,
                    row.installment(), share, percentOf(row, memberId, share),
                    options.showCategories() ? categoryNames.get(row.categoryId()) : null, isPending));
        }
        List<CardGroup> groups = new ArrayList<>();
        linesByCard.forEach((cardId, lines) -> {
            CardView card = cardById.get(cardId);
            lines.sort(Comparator.comparing(CardLine::date).thenComparing(CardLine::description));
            groups.add(new CardGroup(card == null ? String.valueOf(cardId) : card.nickname(),
                    card == null ? List.of() : card.lastDigits(), bills.bill(cardId, period).dueDate(), lines,
                    sum(lines.stream().map(CardLine::amount).toList())));
        });
        groups.sort(Comparator.comparing(CardGroup::cardName, String.CASE_INSENSITIVE_ORDER));

        List<OccurrenceView> occurrences = recurrence.views(period).stream().filter(o -> o.memberId() == memberId).toList();
        List<ExpenseLine> fixed = occurrences.stream()
                .filter(o -> !o.income() && o.paymentMethod() != PaymentMethod.CARD)
                .map(o -> new ExpenseLine(null, o.description(), o.amount(),
                        options.showCategories() ? categoryNames.get(o.categoryId()) : null))
                .toList();
        List<ExpenseLine> variable = expenses.views(period).stream().filter(e -> e.memberId() == memberId)
                .sorted(Comparator.comparing(ExpenseView::expenseDate))
                .map(e -> new ExpenseLine(e.expenseDate(), e.description(), e.amount(),
                        options.showCategories() ? categoryNames.get(e.categoryId()) : null))
                .toList();
        Money income = sum(occurrences.stream().filter(OccurrenceView::income).map(OccurrenceView::amount).toList());
        List<CommittedPeriod> committed = options.includeFuture() ? committed(memberId, period, cardById) : List.of();

        Money cardTotal = sum(groups.stream().map(CardGroup::total).toList());
        Money fixedTotal = sum(fixed.stream().map(ExpenseLine::amount).toList());
        Money variableTotal = sum(variable.stream().map(ExpenseLine::amount).toList());
        Carry previousBalance = carry(carryOvers.incoming(period), memberId, CarryOverView::fromPeriod);
        Carry carriedOut = carry(carryOvers.outgoing(period), memberId, CarryOverView::toPeriod);
        LocalDate dueDate = groups.stream().map(CardGroup::dueDate).filter(Objects::nonNull).min(LocalDate::compareTo)
                .orElse(null);
        return new MemberStatement(member.id(), member.code(), member.name(), member.main(), period, LocalDate.now(clock),
                groups, fixed, variable, committed, income, cardTotal, fixedTotal, variableTotal, previousBalance, carriedOut,
                cardTotal.plus(fixedTotal).plus(variableTotal).plus(previousBalance == null ? Money.ZERO
                        : previousBalance.amount()), dueDate, pixKey(member, payer), pending, options);
    }

    private static Carry carry(List<CarryOverView> carries, long memberId, Function<CarryOverView, BillingPeriod> other) {
        return carries.stream().filter(c -> c.memberId() == memberId).findFirst()
                .map(c -> new Carry(other.apply(c), c.amount())).orElse(null);
    }

    private List<CommittedPeriod> committed(long memberId, BillingPeriod period, Map<Long, CardView> cardById) {
        Map<BillingPeriod, Money> byPeriod = new TreeMap<>();
        cardById.values().stream().filter(c -> c.primaryCardId() == null).forEach(card -> entries
                .list(new EntryFilter(null, card.id(), null, null, null, null, "PROJECTED")).stream()
                .filter(r -> r.billingPeriod().compareTo(period) > 0 && "INSTALLMENT".equals(r.type()))
                .forEach(r -> {
                    Money share = r.memberAmounts(card.defaultMemberId()).get(memberId);
                    if (share != null) {
                        byPeriod.merge(r.billingPeriod(), share, Money::plus);
                    }
                }));
        return byPeriod.entrySet().stream().map(e -> new CommittedPeriod(e.getKey(), e.getValue())).toList();
    }

    private String pixKey(MemberView member, Long payer) {
        if (!member.main() && payer != null) {
            String payerKey = members.find(payer).map(MemberView::pixKey).orElse(null);
            if (payerKey != null && !payerKey.isBlank()) {
                return payerKey;
            }
        }
        return member.pixKey() == null || member.pixKey().isBlank() ? null : member.pixKey();
    }

    private static long payerOf(Map<Long, CardView> cards, long cardId) {
        CardView card = cards.get(cardId);
        return card == null ? 0L : card.defaultMemberId();
    }

    private static BigDecimal percentOf(EntryRow row, long memberId, Money share) {
        if (row.splits().size() <= 1) {
            return null;
        }
        return row.splits().stream().filter(s -> s.memberId() == memberId).map(ReviewItem.Share::percentage)
                .filter(Objects::nonNull).findFirst()
                .orElseGet(() -> row.amount().isZero() ? null
                        : share.amount().multiply(HUNDRED).divide(row.amount().amount(), 1, RoundingMode.HALF_UP));
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money.ZERO, Money::plus);
    }
}

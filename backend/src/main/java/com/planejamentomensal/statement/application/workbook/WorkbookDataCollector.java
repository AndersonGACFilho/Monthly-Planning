package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CardView;
import com.planejamentomensal.card.application.CycleView;
import com.planejamentomensal.carryover.application.CarryOverCommands;
import com.planejamentomensal.carryover.application.CarryOverView;
import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.category.application.CategoryView;
import com.planejamentomensal.entry.application.EntryFilter;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.expense.application.ExpenseView;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.recurrence.application.OccurrenceView;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import com.planejamentomensal.statement.application.workbook.WorkbookData.CardRow;
import com.planejamentomensal.statement.application.workbook.WorkbookData.Line;
import com.planejamentomensal.statement.application.workbook.WorkbookData.Member;
import com.planejamentomensal.statement.application.workbook.WorkbookData.MemberSheet;
import com.planejamentomensal.statement.application.workbook.WorkbookData.MonthBlock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gathers the data of the monthly spreadsheet from the other modules (XLS-02 to XLS-07).
 *
 * <p>Card entries become one row per member share (UNCONFIRMED left out, Q6). Fixed and variable expenses of a
 * third party go to the "Bills" group of the cards sheet with the member code, so they count in that member's
 * total; those of MAIN members go to the monthly expenses sheet (XLS-03).</p>
 *
 * <p>A third party's carry-overs (RF-28) leave the cards sheet alone, so each month keeps its own total: they become
 * income lines beside the reimbursements and, on the member sheets, a previous-balance row and a note below the
 * block.</p>
 *
 * @since 1.0
 */
@Component
@Transactional
public class WorkbookDataCollector {

    private static final int SHEET_NAME_MAX = 31;

    private final MemberQueries members;
    private final CardQueries cards;
    private final CategoryQueries categories;
    private final EntryQueries entries;
    private final RecurrenceCommands recurrence;
    private final VariableExpenseCommands expenses;
    private final CarryOverCommands carryOvers;
    private final Settings settings;

    /**
     * Creates the collector.
     *
     * @param members    member queries
     * @param cards      card queries
     * @param categories category queries
     * @param entries    card entries
     * @param recurrence incomes and fixed expenses
     * @param expenses   variable expenses
     * @param carryOvers carry-overs
     * @param settings   card order, member sheets and neighbouring months
     */
    public WorkbookDataCollector(MemberQueries members, CardQueries cards, CategoryQueries categories, EntryQueries entries,
            RecurrenceCommands recurrence, VariableExpenseCommands expenses, CarryOverCommands carryOvers,
            Settings settings) {
        this.members = members;
        this.cards = cards;
        this.categories = categories;
        this.entries = entries;
        this.recurrence = recurrence;
        this.expenses = expenses;
        this.carryOvers = carryOvers;
        this.settings = settings;
    }

    /**
     * Collects the data of a period.
     *
     * @param period billing period
     * @param labels labels of the spreadsheet language
     * @return data
     */
    public WorkbookData collect(BillingPeriod period, WorkbookLabels labels) {
        List<MemberView> all = members.all().stream()
                .sorted(Comparator.comparing((MemberView m) -> !m.main()).thenComparing(MemberView::code))
                .toList();
        Map<Long, MemberView> byId = new HashMap<>();
        all.forEach(m -> byId.put(m.id(), m));
        Map<Long, String> categoryNames = new HashMap<>();
        categories.all().forEach(c -> categoryNames.put(c.id(), c.name()));
        List<CardView> orderedCards = orderedCards();

        List<CardRow> rows = cardRows(period, orderedCards, byId, categoryNames, m -> true, labels);
        List<OccurrenceView> occurrences = recurrence.views(period);
        List<Line> incomes = occurrences.stream()
                .filter(o -> o.income() && isMain(byId, o.memberId()))
                .map(o -> new Line(o.description(), o.amount())).toList();
        List<Line> monthly = new ArrayList<>();
        occurrences.stream().filter(o -> !o.income() && o.paymentMethod() != PaymentMethod.CARD && isMain(byId, o.memberId()))
                .forEach(o -> monthly.add(new Line(o.description(), o.amount())));
        expenses.views(period).stream().filter(e -> isMain(byId, e.memberId()))
                .forEach(e -> monthly.add(new Line(e.description(), e.amount())));

        List<String> categoryList = categories.all().stream().map(CategoryView::name)
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<Member> memberList = all.stream().map(m -> new Member(m.id(), m.code(), m.name(), m.main())).toList();
        List<Line> carries = new ArrayList<>();
        thirdPartyCarries(carryOvers.incoming(period), byId).forEach(c -> carries.add(new Line(
                labels.get("previousBalanceOf", label(byId.get(c.memberId())), monthYear(c.fromPeriod(), labels)), c.amount())));
        thirdPartyCarries(carryOvers.outgoing(period), byId).forEach(c -> carries.add(new Line(
                labels.get("carriedOutOf", label(byId.get(c.memberId())), monthYear(c.toPeriod(), labels)),
                c.amount().negate())));
        return new WorkbookData(period, memberList, rows, incomes, monthly, carries, categoryList,
                memberSheets(period, orderedCards, byId, categoryNames, labels));
    }

    private List<MemberSheet> memberSheets(BillingPeriod period, List<CardView> orderedCards, Map<Long, MemberView> byId,
            Map<Long, String> categoryNames, WorkbookLabels labels) {
        int before = settings.getInt(Setting.WORKBOOK_MONTHS_BEFORE);
        int after = settings.getInt(Setting.WORKBOOK_MONTHS_AFTER);
        List<MemberSheet> sheets = new ArrayList<>();
        for (Long memberId : settings.getIds(Setting.WORKBOOK_MEMBER_SHEETS)) {
            MemberView member = byId.get(memberId);
            if (member == null) {
                continue;
            }
            List<MonthBlock> blocks = new ArrayList<>();
            for (int i = -before; i <= after; i++) {
                BillingPeriod month = period.plusMonths(i);
                List<CardRow> rows = cardRows(month, orderedCards, byId, categoryNames, m -> m == memberId.longValue(), labels);
                memberCarry(carryOvers.incoming(month), memberId).ifPresent(c -> rows.add(new CardRow(
                        labels.get("previousBalance"), 0, member.code(), "-", monthYear(c.fromPeriod(), labels), c.amount(),
                        null, false)));
                Line carriedOut = memberCarry(carryOvers.outgoing(month), memberId)
                        .map(c -> new Line(labels.get("carriedOut", monthYear(c.toPeriod(), labels)), c.amount())).orElse(null);
                blocks.add(new MonthBlock(month, rows, carriedOut));
            }
            sheets.add(new MemberSheet(sheetName(member.name(), labels.locale()), blocks));
        }
        return sheets;
    }

    private static List<CarryOverView> thirdPartyCarries(List<CarryOverView> carries, Map<Long, MemberView> byId) {
        return carries.stream().filter(c -> byId.containsKey(c.memberId()) && !byId.get(c.memberId()).main())
                .sorted(Comparator.comparing(c -> byId.get(c.memberId()).code())).toList();
    }

    private static Optional<CarryOverView> memberCarry(List<CarryOverView> carries, long memberId) {
        return carries.stream().filter(c -> c.memberId() == memberId).findFirst();
    }

    private static String label(MemberView member) {
        return member.code() + " — " + member.name();
    }

    private static String monthYear(BillingPeriod period, WorkbookLabels labels) {
        return labels.month(period) + " " + period.year();
    }

    private List<CardRow> cardRows(BillingPeriod period, List<CardView> orderedCards, Map<Long, MemberView> byId,
            Map<Long, String> categoryNames, Predicate<Long> memberFilter, WorkbookLabels labels) {
        Map<Long, List<EntryRow>> byCard = new LinkedHashMap<>();
        orderedCards.forEach(c -> byCard.put(c.id(), new ArrayList<>()));
        entries.list(new EntryFilter(period, null, null, null, null, null, null)).stream()
                .filter(r -> !"UNCONFIRMED".equals(r.status()))
                .forEach(r -> byCard.computeIfAbsent(r.cardId(), k -> new ArrayList<>()).add(r));
        Map<Long, CardView> cardById = new HashMap<>();
        orderedCards.forEach(c -> cardById.put(c.id(), c));
        List<CardRow> rows = new ArrayList<>();
        byCard.forEach((cardId, list) -> {
            CardView card = cardById.get(cardId);
            if (card == null) {
                return;
            }
            int dueDay = cards.cycleFor(cardId, period).map(CycleView::dueDay).orElse(0);
            list.sort(Comparator.comparing(EntryRow::entryDate).thenComparing(EntryRow::entryId));
            for (EntryRow row : list) {
                row.memberAmounts(card.defaultMemberId()).forEach((memberId, share) -> {
                    MemberView member = byId.get(memberId);
                    if (member == null || !memberFilter.test(memberId)) {
                        return;
                    }
                    rows.add(new CardRow(card.nickname(), dueDay, member.code(), row.installment(), row.displayDescription(),
                            share, member.main() ? categoryNames.get(row.categoryId()) : null,
                            "PENDING_REVIEW".equals(row.reviewStatus())));
                });
            }
        });
        String bills = labels.get("bills");
        for (OccurrenceView o : recurrence.views(period)) {
            MemberView member = byId.get(o.memberId());
            if (!o.income() && o.paymentMethod() != PaymentMethod.CARD && member != null && !member.main()
                    && memberFilter.test(member.id())) {
                rows.add(new CardRow(bills, o.day(), member.code(), "*", o.description(), o.amount(), null, false));
            }
        }
        for (ExpenseView e : expenses.views(period)) {
            MemberView member = byId.get(e.memberId());
            if (member != null && !member.main() && memberFilter.test(member.id())) {
                rows.add(new CardRow(bills, e.expenseDate().getDayOfMonth(), member.code(), "-", e.description(), e.amount(),
                        null, false));
            }
        }
        return rows;
    }

    private List<CardView> orderedCards() {
        List<Long> order = settings.getIds(Setting.WORKBOOK_CARD_ORDER);
        return cards.all().stream().filter(c -> c.primaryCardId() == null)
                .sorted(Comparator.comparing((CardView c) -> order.contains(c.id()) ? order.indexOf(c.id()) : Integer.MAX_VALUE)
                        .thenComparing(CardView::nickname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static boolean isMain(Map<Long, MemberView> byId, long memberId) {
        MemberView member = byId.get(memberId);
        return member != null && member.main();
    }

    /**
     * Turns a member name into a valid sheet name: capitals, no {@code []:*?/\} and at most 31 characters.
     *
     * @param name   member name
     * @param locale spreadsheet language
     * @return sheet name
     */
    static String sheetName(String name, Locale locale) {
        String clean = name.toUpperCase(locale).replaceAll("[\\[\\]:*?/\\\\']", " ").strip();
        return clean.length() > SHEET_NAME_MAX ? clean.substring(0, SHEET_NAME_MAX) : clean;
    }
}

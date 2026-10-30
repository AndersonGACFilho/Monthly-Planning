package com.planejamentomensal.summary.application;

import com.planejamentomensal.entry.application.EntryRow;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.summary.domain.CategoryTotalsCalculator;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator;
import com.planejamentomensal.summary.domain.CommittedInstallmentsCalculator.FutureInstallment;
import com.planejamentomensal.summary.domain.ExpenseLine;
import com.planejamentomensal.summary.domain.HouseholdTotals;
import com.planejamentomensal.summary.domain.Indicators;
import com.planejamentomensal.summary.domain.MemberKindInfo;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator;
import com.planejamentomensal.summary.domain.SettlementCalculator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the overview of a period; it only gathers data and hands it to small calculators (SOL-01).
 *
 * <p>Household totals (Q8, XLS-04): income = incomes of MAIN members + the third-party share of the expenses
 * (reimbursements), adjusted by what third parties carried in and out (RF-28); expense = fixed (not paid by card) +
 * variable + card bills (RF-11). With a member filter (RF-15) only that member's incomes and shares count.</p>
 *
 * @since 1.0
 */
@Service
@Transactional
public class MonthlySummaryService {

    private final PeriodData data;
    private final CardBillSummaries cards;
    private final MemberQueries members;
    private final Settings settings;

    /**
     * Creates the service.
     *
     * @param data     period data
     * @param cards    card totals
     * @param members  member queries
     * @param settings alert thresholds
     */
    public MonthlySummaryService(PeriodData data, CardBillSummaries cards, MemberQueries members, Settings settings) {
        this.data = data;
        this.cards = cards;
        this.members = members;
        this.settings = settings;
    }

    /**
     * Builds the overview.
     *
     * @param period   billing period
     * @param memberId member filter, or {@code null}
     * @return the overview
     */
    public MonthlySummary summary(BillingPeriod period, Long memberId) {
        List<MemberKindInfo> all = members.all().stream().map(m -> new MemberKindInfo(m.id(), m.main())).toList();
        Set<Long> thirdParties = all.stream().filter(m -> !m.main()).map(MemberKindInfo::id).collect(Collectors.toSet());
        PeriodData.Lines current = data.collect(period);
        PeriodData.Lines previous = data.collect(period.previous());
        List<EntryRow> future = data.futureInstallments(period);

        Predicate<ExpenseLine> lineFilter = l -> memberId == null || l.memberId() == memberId;
        HouseholdTotals totals = HouseholdTotals.of(current.incomes(), current.expenses(), thirdParties, memberId,
                current.carries());
        List<ExpenseLine> lines = totals.lines();
        Indicators indicators = Indicators.of(totals.income(), totals.expense(),
                settings.getInt(Setting.ALERT_WARNING_PERCENT), settings.getInt(Setting.ALERT_DANGER_PERCENT));

        Predicate<EntryRow> entryFilter = r -> memberId == null || r.splits().isEmpty() && data.payerOf(r.cardId()) == memberId
                || r.splits().stream().anyMatch(s -> s.memberId() == memberId);
        List<EntryRow> visible = current.entries().stream().filter(entryFilter).toList();
        List<EntryRow> pending = visible.stream()
                .filter(r -> "PENDING_REVIEW".equals(r.reviewStatus()) && !PeriodData.UNCONFIRMED.equals(r.status())).toList();
        List<MemberKindInfo> shownMembers = memberId == null ? all : all.stream().filter(m -> m.id() == memberId).toList();

        return new MonthlySummary(period, memberId, indicators, totals.mainIncome(), totals.reimbursements(), totals.fixed(),
                totals.variable(), totals.card(),
                CategoryTotalsCalculator.calculate(lines, previous.expenses().stream().filter(lineFilter).toList()),
                MemberTotalsCalculator.calculate(shownMembers, current.incomes(), current.expenses(), current.carries()),
                cards.build(period, current.entries(), future, entryFilter),
                CommittedInstallmentsCalculator.nextPeriods(period, future.stream().filter(entryFilter)
                        .map(r -> new FutureInstallment(r.billingPeriod(), r.cardId(), r.amount())).toList()),
                SettlementCalculator.calculate(current.expenses()),
                pending.size(), sum(pending.stream().map(EntryRow::amount).toList()),
                (int) visible.stream().filter(r -> PeriodData.UNCONFIRMED.equals(r.status())).count());
    }

    private static Money sum(List<Money> amounts) {
        return amounts.stream().reduce(Money.ZERO, Money::plus);
    }
}

package com.planejamentomensal.summary.application;

import com.planejamentomensal.carryover.application.CarryOverCommands;
import com.planejamentomensal.carryover.application.CarryOverView;
import com.planejamentomensal.member.application.MemberQueries;
import com.planejamentomensal.member.application.MemberView;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.summary.domain.MemberKindInfo;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator;
import com.planejamentomensal.summary.domain.MemberTotalsCalculator.MemberTotal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carries what third parties still owe at the end of a period into the next one (RF-28): the expense of the period
 * plus what they owed from before, minus the incomes they paid in it. Carrying again replaces the amount, so a period
 * that changed afterwards can be brought up to date.
 *
 * @since 1.0
 */
@Service
@Transactional
public class CarryOverUseCase {

    private final PeriodData data;
    private final MemberQueries members;
    private final CarryOverCommands carryOvers;

    /**
     * Creates the use case.
     *
     * @param data       period data
     * @param members    member queries
     * @param carryOvers carry-over storage
     */
    public CarryOverUseCase(PeriodData data, MemberQueries members, CarryOverCommands carryOvers) {
        this.data = data;
        this.members = members;
        this.carryOvers = carryOvers;
    }

    /**
     * Carries the unpaid amount of each member into the next period.
     *
     * @param period    source period
     * @param memberIds third-party members
     * @return the carry-overs
     * @throws BusinessRuleException with code {@code carry-over-main-member} for a MAIN member and
     *                               {@code carry-over-nothing-due} when the member owes nothing
     */
    public List<CarryOverView> carry(BillingPeriod period, List<Long> memberIds) {
        PeriodData.Lines lines = data.collect(period);
        return memberIds.stream().distinct().map(memberId -> {
            MemberView member = members.find(memberId).orElseThrow(() -> new NotFoundException("member", memberId));
            if (member.main()) {
                throw new BusinessRuleException("carry-over-main-member");
            }
            MemberTotal total = MemberTotalsCalculator.calculate(List.of(new MemberKindInfo(memberId, false)),
                    lines.incomes(), lines.expenses(), lines.carries()).getFirst();
            return carryOvers.carry(memberId, period, total.unpaid());
        }).toList();
    }

    /**
     * Removes the carry-over of a member out of a period.
     *
     * @param period   source period
     * @param memberId member
     */
    public void undo(BillingPeriod period, long memberId) {
        carryOvers.undo(memberId, period);
    }
}

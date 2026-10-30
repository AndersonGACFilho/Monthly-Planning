package com.planejamentomensal.carryover.application;

import com.planejamentomensal.carryover.domain.CarryOver;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores, removes and lists carry-overs (RF-28). A carry-over that was itself carried on cannot change, or the next
 * period would keep an amount that no longer matches.
 *
 * @since 1.0
 */
@Service
@Transactional
public class CarryOverCommands {

    private final CarryOverRepository carryOvers;

    /**
     * Creates the commands.
     *
     * @param carryOvers carry-over storage
     */
    public CarryOverCommands(CarryOverRepository carryOvers) {
        this.carryOvers = carryOvers;
    }

    /**
     * Lists the carry-overs owed in a period.
     *
     * @param period target period
     * @return carry-overs
     */
    @Transactional(readOnly = true)
    public List<CarryOverView> incoming(BillingPeriod period) {
        return carryOvers.findByToPeriod(period).stream().map(CarryOverView::of).toList();
    }

    /**
     * Lists the carry-overs out of a period.
     *
     * @param period source period
     * @return carry-overs
     */
    @Transactional(readOnly = true)
    public List<CarryOverView> outgoing(BillingPeriod period) {
        return carryOvers.findByFromPeriod(period).stream().map(CarryOverView::of).toList();
    }

    /**
     * Carries an amount of a member into the next period, replacing the one carried before.
     *
     * @param memberId member who owes
     * @param from     source period
     * @param amount   amount still owed
     * @return the carry-over
     */
    public CarryOverView carry(long memberId, BillingPeriod from, Money amount) {
        CarryOver carryOver = carryOvers.findByMemberIdAndFromPeriod(memberId, from).map(existing -> {
            checkNotCarriedOn(existing);
            existing.changeAmount(amount);
            return existing;
        }).orElseGet(() -> new CarryOver(memberId, from, amount));
        return CarryOverView.of(carryOvers.saveAndFlush(carryOver));
    }

    /**
     * Removes the carry-over of a member out of a period.
     *
     * @param memberId member
     * @param from     source period
     */
    public void undo(long memberId, BillingPeriod from) {
        CarryOver carryOver = carryOvers.findByMemberIdAndFromPeriod(memberId, from)
                .orElseThrow(() -> new NotFoundException("carry-over", memberId + "@" + from));
        checkNotCarriedOn(carryOver);
        carryOvers.delete(carryOver);
    }

    private void checkNotCarriedOn(CarryOver carryOver) {
        if (carryOvers.findByMemberIdAndFromPeriod(carryOver.memberId(), carryOver.toPeriod()).isPresent()) {
            throw new BusinessRuleException("carry-over-chained", carryOver.toPeriod());
        }
    }
}

package com.planejamentomensal.entry.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CycleView;
import com.planejamentomensal.entry.domain.BillStatus;
import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Versions;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bills of a card per billing period, with dates forecast from the card cycle while no file was imported
 * (RF-36), and their payment.
 *
 * @since 1.0
 */
@Service
@Transactional
public class CardBillQueries {

    private final CardBillRepository bills;
    private final CardQueries cards;

    /**
     * Creates the queries.
     *
     * @param bills bill storage
     * @param cards card queries
     */
    public CardBillQueries(CardBillRepository bills, CardQueries cards) {
        this.bills = bills;
        this.cards = cards;
    }

    /**
     * Returns the bill of a primary card in a period, stored or forecast.
     *
     * @param cardId primary card
     * @param period billing period
     * @return the bill
     */
    @Transactional(readOnly = true)
    public CardBillView bill(long cardId, BillingPeriod period) {
        CycleView cycle = cards.cycleFor(cardId, period).orElse(null);
        LocalDate forecastDue = cycle == null ? null : period.atDay(cycle.dueDay());
        return bills.findByCardIdAndBillingPeriod(cardId, period)
                .map(b -> {
                    LocalDate due = b.dueDate() != null ? b.dueDate() : forecastDue;
                    LocalDate closing = b.closingDate() != null ? b.closingDate() : forecastClosing(cycle, due);
                    return new CardBillView(b.id(), cardId, period, closing, due, b.status().name(), b.partial(),
                            b.fileTotal(), b.version());
                })
                .orElseGet(() -> new CardBillView(null, cardId, period, forecastClosing(cycle, forecastDue), forecastDue,
                        BillStatus.OPEN.name(), false, null, 0));
    }

    /**
     * Marks a bill as paid, open or closed; a bill known only by forecast is stored first (RF-36, IMP-66).
     *
     * @param cardId          primary card
     * @param period          billing period
     * @param expectedVersion version the client saw, or {@code null}
     * @return the bill
     * @throws BusinessRuleException with code {@code bill-already-paid} if the bill was paid before
     */
    public CardBillView markPaid(long cardId, BillingPeriod period, Long expectedVersion) {
        CardBill bill = bills.findByCardIdAndBillingPeriod(cardId, period).orElseGet(() -> new CardBill(cardId, period));
        if (bill.status() == BillStatus.PAID) {
            throw new BusinessRuleException("bill-already-paid", cardId, period);
        }
        Versions.check(expectedVersion, bill.version());
        bill.markPaid();
        bills.save(bill);
        return bill(cardId, period);
    }

    /**
     * Closing forecast from the card cycle before the bill's due date, so a due date read from a file moves the
     * closing with it (RN-22).
     *
     * @param cycle card cycle of the period, or {@code null}
     * @param due   due date of the bill, or {@code null}
     * @return closing date, or {@code null} without a cycle or a due date
     */
    private static LocalDate forecastClosing(CycleView cycle, LocalDate due) {
        return cycle == null || due == null ? null : closingBefore(due, cycle.closingDay());
    }

    /**
     * Latest closing day on or before the due date (the closing that opens the payment window).
     *
     * @param due        due date
     * @param closingDay typical closing day
     * @return closing date
     */
    static LocalDate closingBefore(LocalDate due, int closingDay) {
        BillingPeriod month = BillingPeriod.ofDueDate(due);
        LocalDate sameMonth = month.atDay(closingDay);
        return sameMonth.isBefore(due) ? sameMonth : month.previous().atDay(closingDay);
    }
}

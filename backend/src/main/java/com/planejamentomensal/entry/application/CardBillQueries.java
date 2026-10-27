package com.planejamentomensal.entry.application;

import com.planejamentomensal.card.application.CardQueries;
import com.planejamentomensal.card.application.CycleView;
import com.planejamentomensal.entry.domain.BillStatus;
import com.planejamentomensal.entry.domain.CardBill;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Versions;
import java.time.LocalDate;
import java.util.Optional;
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
        Optional<CycleView> cycle = cards.cycleFor(cardId, period);
        LocalDate forecastDue = cycle.map(c -> period.atDay(c.dueDay())).orElse(null);
        LocalDate forecastClosing = cycle.map(c -> closingBefore(forecastDue, c.closingDay())).orElse(null);
        return bills.findByCardIdAndBillingPeriod(cardId, period)
                .map(b -> new CardBillView(b.id(), cardId, period, b.closingDate() != null ? b.closingDate() : forecastClosing,
                        b.dueDate() != null ? b.dueDate() : forecastDue, b.status().name(), b.partial(), b.fileTotal(),
                        b.version()))
                .orElseGet(() -> new CardBillView(null, cardId, period, forecastClosing, forecastDue, BillStatus.OPEN.name(),
                        false, null, 0));
    }

    /**
     * Marks a closed bill as paid (RF-36).
     *
     * @param cardId          primary card
     * @param period          billing period
     * @param expectedVersion version the client saw, or {@code null}
     * @return the bill
     * @throws BusinessRuleException with code {@code bill-not-closed} unless the bill is closed
     */
    public CardBillView markPaid(long cardId, BillingPeriod period, Long expectedVersion) {
        CardBill bill = bills.findByCardIdAndBillingPeriod(cardId, period)
                .filter(b -> b.status() == BillStatus.CLOSED)
                .orElseThrow(() -> new BusinessRuleException("bill-not-closed", cardId, period));
        Versions.check(expectedVersion, bill.version());
        bill.markPaid();
        bills.save(bill);
        return bill(cardId, period);
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

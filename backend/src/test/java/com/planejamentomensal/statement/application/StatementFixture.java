package com.planejamentomensal.statement.application;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.entry.application.EntryCommands;
import com.planejamentomensal.entry.application.EntryLedger;
import com.planejamentomensal.entry.application.LedgerFixture;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.domain.BillEntry;
import com.planejamentomensal.expense.application.VariableExpenseCommands;
import com.planejamentomensal.expense.domain.ExpenseDetails;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.Recurrence;
import com.planejamentomensal.recurrence.domain.RuleDetails;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.shared.domain.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Statement scenario shared by tests (TST-05): a house and a third party with card entries, a split purchase, a
 * fixed and a variable expense of the third party and a future installment.
 */
public final class StatementFixture {

    /** Billing period of the scenario. */
    public static final BillingPeriod NOV = BillingPeriod.of(2026, 11);

    /** Members and card. */
    public final LedgerFixture f;

    /**
     * Creates the scenario.
     *
     * @param members    member use case
     * @param cards      card use case
     * @param ledger     ledger
     * @param entries    entry commands
     * @param recurrence recurrence commands
     * @param expenses   variable expenses
     * @param suffix     unique digit
     */
    public StatementFixture(SaveMemberUseCase members, SaveCardUseCase cards, EntryLedger ledger, EntryCommands entries,
            RecurrenceCommands recurrence, VariableExpenseCommands expenses, String suffix) {
        f = new LedgerFixture(members, cards, suffix);
        BillEntry shared = ledger.addPurchase(null, f.purchase("MERCADO BOM", "100.00", LocalDate.of(2026, 10, 5), NOV, null, null, "PURCHASE"));
        entries.classify(shared.id(), null, false, List.of(new ReviewItem.Share(f.main, new BigDecimal("50"), null),
                new ReviewItem.Share(f.thirdParty, new BigDecimal("50"), null)), null, null);
        BillEntry hers = ledger.addPurchase(null, f.purchase("FARMACIA", "80.00", LocalDate.of(2026, 10, 7), NOV, 1, 2, "PURCHASE"));
        entries.classify(hers.id(), null, false, List.of(new ReviewItem.Share(f.thirdParty, new BigDecimal("100"), null)), null, null);
        ledger.addPurchase(null, f.purchase("POSTO", "200.00", LocalDate.of(2026, 10, 8), NOV, null, null, "PURCHASE"));
        recurrence.create(RuleKind.FIXED_EXPENSE, new RuleDetails("Plano de saúde", f.thirdParty, null, null,
                Money.of("300.00"), Recurrence.MONTHLY, 1, 10, NOV, null, PaymentMethod.BOLETO));
        expenses.create(new ExpenseDetails(LocalDate.of(2026, 11, 4), null, "Remédio", f.thirdParty, null,
                Money.of("25.50"), PaymentMethod.PIX));
    }
}

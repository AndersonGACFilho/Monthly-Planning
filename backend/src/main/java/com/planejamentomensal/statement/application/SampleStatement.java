package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import com.planejamentomensal.statement.domain.MemberStatement;
import com.planejamentomensal.statement.domain.MemberStatement.CardGroup;
import com.planejamentomensal.statement.domain.MemberStatement.CardLine;
import com.planejamentomensal.statement.domain.MemberStatement.CommittedPeriod;
import com.planejamentomensal.statement.domain.MemberStatement.ExpenseLine;
import com.planejamentomensal.statement.domain.StatementOptions;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Made-up statement for the template editor preview when no real member or period is chosen (TPL-03).
 *
 * @since 1.0
 */
public final class SampleStatement {

    private SampleStatement() {
    }

    /**
     * Builds the sample.
     *
     * @param period billing period shown
     * @return a statement with every block filled
     */
    public static MemberStatement of(BillingPeriod period) {
        LocalDate day = period.atDay(1).minusMonths(1);
        List<CardLine> lines = List.of(
                new CardLine(day.plusDays(2), "Supermercado", "-", Money.of("62.40"), new BigDecimal("50"), "Mercado", false),
                new CardLine(day.plusDays(9), "Farmácia", "02/03", Money.of("45.90"), null, "Saúde", false),
                new CardLine(day.plusDays(15), "Streaming", "*", Money.of("21.90"), null, "Lazer", true));
        CardGroup card = new CardGroup("Itaú", List.of("1234"), period.atDay(10), lines, Money.of("130.20"));
        List<ExpenseLine> fixed = List.of(new ExpenseLine(null, "Plano de saúde", Money.of("300.00"), "Saúde"));
        List<ExpenseLine> variable = List.of(new ExpenseLine(period.atDay(4), "Feira", Money.of("35.00"), "Mercado"));
        return new MemberStatement(0, "M", "Maria", false, period, LocalDate.now(), List.of(card), fixed, variable,
                List.of(new CommittedPeriod(period.next(), Money.of("45.90"))), Money.ZERO, Money.of("130.20"),
                Money.of("300.00"), Money.of("35.00"), null, null, Money.of("465.20"), period.atDay(10), "maria@example.com",
                List.of("Streaming"), StatementOptions.DEFAULT);
    }
}

package com.planejamentomensal.imports.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything read from one bill file (RF-63, RF-64).
 *
 * @param format              file format
 * @param open                {@code true} for an open (partial) bill, {@code false} for a closed one (RF-63f)
 * @param billingPeriod       period of the bill's due date
 * @param dueDate             due date
 * @param closingDate         closing date of this bill, or {@code null} when the file does not show it
 * @param nextClosingForecast "Previsão próx. Fechamento" (PDF, RN-22), or {@code null}
 * @param cardLastDigits      card final printed in the header, or {@code null}
 * @param fileTotal           total printed for the bill ("Total desta fatura" or "Valor (parcial)")
 * @param entriesTotal        "Total dos lançamentos atuais" (PDF), or {@code null}
 * @param sectionTotals       printed and computed totals per section (RF-64h)
 * @param lines               lines that may become entries; payments are not included (RF-63d)
 * @param futureInstallments  "Compras parceladas - próximas faturas" (RF-64g), empty for XLSX
 * @since 1.0
 */
public record ParsedBill(BillFormat format, boolean open, BillingPeriod billingPeriod, LocalDate dueDate,
        LocalDate closingDate, LocalDate nextClosingForecast, String cardLastDigits, Money fileTotal,
        Money entriesTotal, List<SectionTotal> sectionTotals, List<RawBillLine> lines,
        List<RawBillLine> futureInstallments) {

    /**
     * Returns the sum of the lines read.
     *
     * @return computed total of the bill's entries (RF-71)
     */
    public Money computedTotal() {
        return lines.stream().map(RawBillLine::amount).reduce(Money.ZERO, Money::plus);
    }

    /**
     * Tells whether every printed total matches the lines read (RF-64h). Open bills have no reliable total
     * and always pass (RF-63b).
     *
     * @return {@code true} if confirmation may proceed
     */
    public boolean totalsMatch() {
        boolean sections = sectionTotals.stream().allMatch(SectionTotal::matches);
        boolean overall = entriesTotal == null || entriesTotal.equals(computedTotal());
        return sections && overall;
    }
}

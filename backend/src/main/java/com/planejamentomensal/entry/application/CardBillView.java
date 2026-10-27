package com.planejamentomensal.entry.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;

/**
 * Read model of the bill of a card in a billing period, for other modules (SOL-06).
 *
 * @param cardBillId    stored bill, or {@code null} while nothing was imported or projected
 * @param cardId        primary card
 * @param billingPeriod period
 * @param closingDate   closing date read from a file or forecast from the card cycle
 * @param dueDate       due date read from a file or forecast from the card cycle
 * @param status        {@code OPEN}, {@code CLOSED} or {@code PAID}
 * @param partial       {@code true} while only an open bill (XLSX) was imported
 * @param fileTotal     total printed on the imported file, or {@code null}
 * @param version       version of the stored bill
 * @since 1.0
 */
public record CardBillView(Long cardBillId, long cardId, BillingPeriod billingPeriod, LocalDate closingDate,
        LocalDate dueDate, String status, boolean partial, Money fileTotal, long version) {
}

package com.planejamentomensal.summary.domain;

import java.time.LocalDate;

/**
 * Record an expense line was taken from, so a total can be opened item by item (RF-12, RF-38).
 *
 * @param referenceId occurrence of a fixed expense, variable expense or card entry, depending on the line source
 * @param description description shown to the user (alias for card entries)
 * @param date        due day of a fixed expense, date of a variable expense or entry date on the bill
 * @param installment {@code n/N}, {@code -} or {@code *} for card entries, otherwise {@code null}
 * @since 1.0
 */
public record ExpenseOrigin(long referenceId, String description, LocalDate date, String installment) {
}

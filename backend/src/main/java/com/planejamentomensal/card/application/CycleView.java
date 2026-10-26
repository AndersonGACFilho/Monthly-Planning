package com.planejamentomensal.card.application;

import com.planejamentomensal.shared.domain.BillingPeriod;

/**
 * Closing and due day of a card in a billing period (RF-04).
 *
 * @param closingDay day the bill closes
 * @param dueDay     day the bill is due
 * @param validFrom  first period of this cycle
 * @since 1.0
 */
public record CycleView(int closingDay, int dueDay, BillingPeriod validFrom) {
}

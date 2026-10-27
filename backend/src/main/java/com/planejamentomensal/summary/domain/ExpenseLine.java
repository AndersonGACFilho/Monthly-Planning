package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;

/**
 * One expense amount attributed to one member, the unit every total is built from (RF-11, RF-25).
 *
 * @param source     fixed, variable or card
 * @param memberId   member who bears the amount
 * @param categoryId category, or {@code null} when not classified
 * @param amount     amount; negative for refunds and credits
 * @param cardId     primary card of a card line, otherwise {@code null}
 * @param payerId    member who paid: the card's default member for card lines, otherwise the member
 * @since 1.0
 */
public record ExpenseLine(ExpenseSource source, long memberId, Long categoryId, Money amount, Long cardId, long payerId) {
}

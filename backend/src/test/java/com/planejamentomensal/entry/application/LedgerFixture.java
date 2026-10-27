package com.planejamentomensal.entry.application;

import com.planejamentomensal.card.application.SaveCardUseCase;
import com.planejamentomensal.card.domain.Bank;
import com.planejamentomensal.card.domain.CardDetails;
import com.planejamentomensal.member.application.SaveMemberUseCase;
import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.time.LocalDate;
import java.util.List;

/**
 * Members, a card and purchases for ledger tests (TST-05).
 */
public final class LedgerFixture {

    /** Main member. */
    public final long main;
    /** Third-party member. */
    public final long thirdParty;
    /** Card. */
    public final long card;

    /**
     * Creates a main member, a third party and a card.
     *
     * @param members member use case
     * @param cards   card use case
     * @param suffix  makes codes and finals unique per test
     */
    public LedgerFixture(SaveMemberUseCase members, SaveCardUseCase cards, String suffix) {
        main = members.create(new MemberDetails("M" + suffix, "Casa", MemberKind.MAIN, "#000000", null, null, null, "pt-BR")).id();
        thirdParty = members.create(new MemberDetails("T" + suffix, "Tia", MemberKind.THIRD_PARTY, "#111111", null, null, null, "pt-BR")).id();
        card = cards.create(new CardDetails("Card " + suffix, Bank.ITAU, List.of("8" + suffix + "00".substring(suffix.length() - 1)),
                "HOLDER", null, main), 3, 10).id();
    }

    /**
     * A purchase to record.
     *
     * @param description description
     * @param amount      amount
     * @param date        purchase date
     * @param period      billing period
     * @param n           installment, or {@code null}
     * @param count       count, or {@code null}
     * @param kind        kind
     * @return the purchase data
     */
    public NewPurchase purchase(String description, String amount, LocalDate date, BillingPeriod period, Integer n,
            Integer count, String kind) {
        return new NewPurchase(card, null, false, date, description, n, count, Money.of(amount), kind, null, null, period,
                date, 1, main, null, false);
    }
}

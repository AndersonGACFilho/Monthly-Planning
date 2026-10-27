package com.planejamentomensal.summary.api;

import com.planejamentomensal.entry.application.CardBillQueries;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.summary.application.CardBillSummaries;
import com.planejamentomensal.summary.application.CardBillSummary;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Card totals of a billing period with closing, due date and status (RF-36, RF-37, IMP-31).
 *
 * @since 1.0
 */
@RestController
public class CardBillController {

    private final CardBillSummaries summaries;
    private final CardBillQueries bills;
    private final CardBillModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param summaries card totals
     * @param bills     card bills
     * @param assembler HAL assembler
     */
    public CardBillController(CardBillSummaries summaries, CardBillQueries bills, CardBillModelAssembler assembler) {
        this.summaries = summaries;
        this.bills = bills;
        this.assembler = assembler;
    }

    /**
     * Lists the card totals of a period, one per active card and per card with entries.
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @return card totals by nickname
     */
    @GetMapping("/api/card-bills")
    public CollectionModel<CardBillModel> cardBills(@RequestParam String billingPeriod) {
        BillingPeriod period = BillingPeriod.parse(billingPeriod);
        return assembler.toCollectionModel(summaries.forPeriod(period))
                .add(Link.of("/api/card-bills?billingPeriod=" + period).withSelfRel());
    }

    /**
     * Returns the total of one card in a period.
     *
     * @param cardId        primary card
     * @param billingPeriod period, {@code yyyy-MM}
     * @return the card total with the bill version as {@code ETag}
     */
    @GetMapping("/api/cards/{cardId}/bills/{billingPeriod}")
    public ResponseEntity<CardBillModel> cardBill(@PathVariable long cardId, @PathVariable String billingPeriod) {
        CardBillSummary summary = find(cardId, BillingPeriod.parse(billingPeriod));
        return ETags.ok(assembler.toModel(summary), summary.bill().version());
    }

    /**
     * Marks a closed bill as paid (RF-36).
     *
     * @param cardId        primary card
     * @param billingPeriod period, {@code yyyy-MM}
     * @param ifMatch       version being edited
     * @return the updated card total
     */
    @PostMapping("/api/cards/{cardId}/bills/{billingPeriod}/payment")
    public ResponseEntity<CardBillModel> payCardBill(@PathVariable long cardId, @PathVariable String billingPeriod,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        BillingPeriod period = BillingPeriod.parse(billingPeriod);
        bills.markPaid(cardId, period, ETags.expectedVersion(ifMatch));
        CardBillSummary summary = find(cardId, period);
        return ETags.ok(assembler.toModel(summary), summary.bill().version());
    }

    private CardBillSummary find(long cardId, BillingPeriod period) {
        return summaries.forPeriod(period).stream().filter(s -> s.cardId() == cardId).findFirst()
                .orElseThrow(() -> new NotFoundException("card", cardId));
    }

    /**
     * Adds {@code card-bills} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor cardBillsRootLink() {
        return () -> Link.of("/api/card-bills{?billingPeriod}", "card-bills");
    }
}

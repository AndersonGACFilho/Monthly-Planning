package com.planejamentomensal.recurrence.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.recurrence.application.RecurrenceCommands;
import com.planejamentomensal.recurrence.domain.Occurrence;
import com.planejamentomensal.recurrence.domain.RuleKind;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Incomes and fixed expenses of a billing period, generated from their rules on first read (RF-22, RF-24,
 * RF-31 to RF-33, RN-30).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/occurrences")
public class OccurrenceController {

    private final RecurrenceCommands commands;
    private final OccurrenceModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param commands  recurrence commands
     * @param assembler HAL assembler
     */
    public OccurrenceController(RecurrenceCommands commands, OccurrenceModelAssembler assembler) {
        this.commands = commands;
        this.assembler = assembler;
    }

    /**
     * Lists the incomes or fixed expenses of a period, generating the missing ones; card-paid expenses are linked
     * to their bill entry (RF-33).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param kind          {@code INCOME} or {@code FIXED_EXPENSE}
     * @return occurrences, by description
     */
    @GetMapping
    public CollectionModel<OccurrenceModel> occurrences(@RequestParam String billingPeriod, @RequestParam RuleKind kind) {
        BillingPeriod period = BillingPeriod.parse(billingPeriod);
        return assembler.toCollectionModel(commands.occurrences(period, kind))
                .add(Link.of("/api/occurrences?billingPeriod=" + period + "&kind=" + kind).withSelfRel());
    }

    /**
     * Lists the card-paid fixed expenses waiting for the user's choice of entry, for the review queue (RN-30).
     *
     * @return occurrences with candidates
     */
    @GetMapping("/pending-links")
    public CollectionModel<OccurrenceModel> pendingOccurrenceLinks() {
        return assembler.toCollectionModel(commands.pendingLinks())
                .add(linkTo(methodOn(OccurrenceController.class).pendingOccurrenceLinks()).withSelfRel());
    }

    /**
     * Returns one occurrence with its version as {@code ETag}.
     *
     * @param id occurrence id
     * @return the occurrence
     */
    @GetMapping("/{id}")
    public ResponseEntity<OccurrenceModel> occurrence(@PathVariable long id) {
        Occurrence occurrence = commands.occurrence(id);
        return ETags.ok(assembler.toModel(occurrence), occurrence.version());
    }

    /**
     * Changes the amount of this period only, keeping the rule's default (RF-22, RF-31).
     *
     * @param id      occurrence id
     * @param request amount
     * @param ifMatch version being edited
     * @return the updated occurrence
     */
    @PutMapping("/{id}/amount")
    public ResponseEntity<OccurrenceModel> overrideOccurrenceAmount(@PathVariable long id,
            @Valid @RequestBody AmountRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Occurrence occurrence = commands.overrideAmount(id, Money.of(request.amount()), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(occurrence), occurrence.version());
    }

    /**
     * Marks an income received or an expense paid, with its date, or back to expected (RF-24, RF-32).
     *
     * @param id      occurrence id
     * @param request flag and date
     * @return the updated occurrence
     */
    @PostMapping("/{id}/status")
    public ResponseEntity<OccurrenceModel> changeOccurrenceStatus(@PathVariable long id,
            @Valid @RequestBody StatusRequest request) {
        Occurrence occurrence = commands.markDone(id, request.done(), request.date());
        return ETags.ok(assembler.toModel(occurrence), occurrence.version());
    }

    /**
     * Links a card-paid fixed expense to the entry chosen among its candidates (RN-30).
     *
     * @param id      occurrence id
     * @param request chosen entry
     * @return the updated occurrence
     */
    @PostMapping("/{id}/link")
    public ResponseEntity<OccurrenceModel> chooseOccurrenceLink(@PathVariable long id,
            @Valid @RequestBody LinkRequest request) {
        Occurrence occurrence = commands.chooseLink(id, request.entryId());
        return ETags.ok(assembler.toModel(occurrence), occurrence.version());
    }

    /**
     * Adds {@code occurrences} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor occurrencesRootLink() {
        return () -> Link.of("/api/occurrences{?billingPeriod,kind}", "occurrences");
    }
}

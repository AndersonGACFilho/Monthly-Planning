package com.planejamentomensal.summary.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.summary.application.MonthlySummary;
import com.planejamentomensal.summary.application.MonthlySummaryService;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Overview of a billing period: indicators, categories, members, cards and what is committed ahead
 * (RF-10 to RF-17, IMP-32).
 *
 * @since 1.0
 */
@RestController
public class MonthlySummaryController {

    private final MonthlySummaryService service;
    private final CardBillModelAssembler cards;

    /**
     * Creates the controller.
     *
     * @param service overview service
     * @param cards   card total assembler
     */
    public MonthlySummaryController(MonthlySummaryService service, CardBillModelAssembler cards) {
        this.service = service;
        this.cards = cards;
    }

    /**
     * Returns the overview of a period, optionally for one member (RF-15).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param member        member filter, optional
     * @return the overview with {@code previous}, {@code next}, {@code by-member} and {@code by-category}
     */
    @GetMapping("/api/summaries/{billingPeriod}")
    public MonthlySummaryModel monthlySummary(@PathVariable String billingPeriod,
            @RequestParam(required = false) Long member) {
        BillingPeriod period = BillingPeriod.parse(billingPeriod);
        MonthlySummary summary = service.summary(period, member);
        MonthlySummaryModel model = new MonthlySummaryModel(summary, summary.cards().stream().map(cards::toModel).toList());
        model.add(linkTo(methodOn(MonthlySummaryController.class).monthlySummary(billingPeriod, member)).withSelfRel().expand());
        model.add(linkTo(methodOn(MonthlySummaryController.class).monthlySummary(period.previous().toString(), member))
                .withRel(Relations.PREVIOUS).expand());
        model.add(linkTo(methodOn(MonthlySummaryController.class).monthlySummary(period.next().toString(), member))
                .withRel(Relations.NEXT).expand());
        model.add(linkTo(methodOn(MonthlySummaryController.class).summaryByMember(billingPeriod)).withRel(Relations.BY_MEMBER));
        model.add(linkTo(methodOn(MonthlySummaryController.class).summaryByCategory(billingPeriod, member))
                .withRel(Relations.BY_CATEGORY).expand());
        model.add(Link.of("/api/card-bills?billingPeriod=" + period, "card-bills"));
        model.add(Link.of("/api/summaries/" + period + "/workbook", Relations.EXPORT_XLSX));
        return model;
    }

    /**
     * Returns the totals by member of a period (RF-13, RF-25).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @return totals by member
     */
    @GetMapping("/api/summaries/{billingPeriod}/members")
    public List<MonthlySummaryModel.MemberRow> summaryByMember(@PathVariable String billingPeriod) {
        return service.summary(BillingPeriod.parse(billingPeriod), null).members().stream()
                .map(MonthlySummaryModel.MemberRow::of).toList();
    }

    /**
     * Returns the expense by category of a period (RF-12).
     *
     * @param billingPeriod period, {@code yyyy-MM}
     * @param member        member filter, optional
     * @return expense by category, largest first
     */
    @GetMapping("/api/summaries/{billingPeriod}/categories")
    public List<MonthlySummaryModel.CategoryRow> summaryByCategory(@PathVariable String billingPeriod,
            @RequestParam(required = false) Long member) {
        return service.summary(BillingPeriod.parse(billingPeriod), member).categories().stream()
                .map(MonthlySummaryModel.CategoryRow::of).toList();
    }

    /**
     * Adds {@code summaries} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor summariesRootLink() {
        return () -> Link.of("/api/summaries/{billingPeriod}{?member}", "summaries");
    }
}

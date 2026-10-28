package com.planejamentomensal.summary.api;

import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.summary.application.HistoryService;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * History of past months: income, expense and balance per month, by member and by category, for charts by month,
 * quarter, semester or year.
 *
 * @since 1.0
 */
@RestController
public class HistoryController {

    private final HistoryService history;
    private final Clock clock;

    /**
     * Creates the controller.
     *
     * @param history history service
     * @param clock   clock for the default range
     */
    public HistoryController(HistoryService history, Clock clock) {
        this.history = history;
        this.clock = clock;
    }

    /**
     * Returns the months of a range, by default the last twelve up to the current one.
     *
     * @param from     first period, {@code yyyy-MM}, optional
     * @param to       last period, {@code yyyy-MM}, optional
     * @param member   member filter, optional
     * @param category category filter with its subcategories, optional
     * @return the history
     */
    @GetMapping("/api/history")
    public HistoryModel history(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) Long member, @RequestParam(required = false) Long category) {
        LocalDate today = LocalDate.now(clock);
        BillingPeriod last = to == null || to.isBlank() ? BillingPeriod.of(today.getYear(), today.getMonthValue()) : BillingPeriod.parse(to);
        BillingPeriod first = from == null || from.isBlank() ? last.plusMonths(-11) : BillingPeriod.parse(from);
        HistoryModel model = new HistoryModel(first.toString(), last.toString(), member, category,
                history.history(first, last, member, category));
        model.add(Link.of("/api/history?from=" + first + "&to=" + last
                + (member == null ? "" : "&member=" + member) + (category == null ? "" : "&category=" + category)).withSelfRel());
        return model;
    }

    /**
     * Adds {@code history} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor historyRootLink() {
        return () -> Link.of("/api/history{?from,to,member,category}", "history");
    }
}

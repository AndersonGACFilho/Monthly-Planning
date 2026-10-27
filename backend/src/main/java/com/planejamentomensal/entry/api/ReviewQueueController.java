package com.planejamentomensal.entry.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.entry.application.ReviewQueue;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Review queue: imported purchases waiting for the user to check member, split, category and alias
 * (RF-74 to RF-79a).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/review-queue")
public class ReviewQueueController {

    private final ReviewQueue queue;

    /**
     * Creates the controller.
     *
     * @param queue review queue
     */
    public ReviewQueueController(ReviewQueue queue) {
        this.queue = queue;
    }

    /**
     * Number and total of pending items (RF-79, RF-79a).
     *
     * @param count number of pending purchases
     * @param total sum of their entries
     */
    public record SummaryModel(int count, BigDecimal total) {
    }

    /**
     * Lists pending items, filtered by card, billing period and import batch (RF-79).
     *
     * @param card          card id, optional
     * @param billingPeriod period {@code yyyy-MM}, optional
     * @param batch         import batch id, optional
     * @return pending items with {@code validate-selected} when there are any
     */
    @GetMapping
    public CollectionModel<ReviewItemModel> reviewQueue(@RequestParam(required = false) Long card,
            @RequestParam(required = false) String billingPeriod, @RequestParam(required = false) Long batch) {
        List<ReviewItemModel> items = queue.pending(card, billingPeriod == null ? null : BillingPeriod.parse(billingPeriod), batch)
                .stream().map(this::model).toList();
        CollectionModel<ReviewItemModel> model = CollectionModel.of(items,
                linkTo(methodOn(ReviewQueueController.class).reviewQueue(card, billingPeriod, batch)).withSelfRel(),
                linkTo(methodOn(ReviewQueueController.class).reviewSummary(null)).withRel("summary"));
        if (!items.isEmpty()) {
            model.add(linkTo(methodOn(ReviewQueueController.class).validateSelected(null)).withRel(Relations.VALIDATE_SELECTED));
        }
        return model;
    }

    /**
     * Count and total of pending items, for the sidebar counter (RF-79) and the overview (RF-79a).
     *
     * @param billingPeriod period {@code yyyy-MM}, optional
     * @return count and total
     */
    @GetMapping("/summary")
    public EntityModel<SummaryModel> reviewSummary(@RequestParam(required = false) String billingPeriod) {
        ReviewQueue.Summary summary = queue.summary(billingPeriod == null ? null : BillingPeriod.parse(billingPeriod));
        return EntityModel.of(new SummaryModel(summary.count(), summary.total().amount()),
                linkTo(methodOn(ReviewQueueController.class).reviewSummary(billingPeriod)).withSelfRel());
    }

    /**
     * Applies changes to one item and validates it (RF-76).
     *
     * @param purchaseId purchase
     * @param request    changes
     * @param ifMatch    version being edited
     * @return the validated item
     */
    @PostMapping("/{purchaseId}/validation")
    public ResponseEntity<ReviewItemModel> validateItem(@PathVariable long purchaseId,
            @Valid @RequestBody ReviewEditRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ReviewItem item = queue.apply(List.of(purchaseId), request.toEdit(), true, ETags.expectedVersion(ifMatch)).getFirst();
        return ETags.ok(model(item), item.version());
    }

    /**
     * Applies the same changes to the selected items and optionally validates them (RF-77).
     *
     * @param request selection, changes and whether to validate
     * @return the updated items
     */
    @PostMapping("/validations")
    public CollectionModel<ReviewItemModel> validateSelected(@Valid @RequestBody BatchValidationRequest request) {
        return CollectionModel.of(queue.apply(request.purchaseIds(), request.changes().toEdit(), request.validate(), null)
                .stream().map(this::model).toList());
    }

    private ReviewItemModel model(ReviewItem item) {
        ReviewItemModel model = new ReviewItemModel(item);
        model.add(Link.of("/api/purchases/" + item.purchaseId(), "purchase"));
        model.add(linkTo(methodOn(ReviewQueueController.class).validateItem(item.purchaseId(), null, null))
                .withRel(Relations.VALIDATE));
        return model;
    }

    /**
     * Adds {@code review-queue} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor reviewQueueRootLink() {
        return () -> Link.of("/api/review-queue", "review-queue");
    }
}

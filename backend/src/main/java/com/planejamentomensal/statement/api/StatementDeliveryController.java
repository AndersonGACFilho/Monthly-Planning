package com.planejamentomensal.statement.api;

import com.planejamentomensal.shared.api.Relations;
import com.planejamentomensal.shared.api.RootLinkContributor;
import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.statement.application.SendStatementUseCase;
import com.planejamentomensal.statement.application.StatementDeliveries;
import com.planejamentomensal.statement.application.StatementDeliveries.HistoryRow;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sends statements in batch and keeps the delivery history with resend and manual confirmation (RF-86 to RF-93,
 * SOL-03).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/statement-deliveries")
public class StatementDeliveryController {

    private final SendStatementUseCase send;
    private final StatementDeliveries deliveries;

    /**
     * Creates the controller.
     *
     * @param send       send use case
     * @param deliveries history
     */
    public StatementDeliveryController(SendStatementUseCase send, StatementDeliveries deliveries) {
        this.send = send;
        this.deliveries = deliveries;
    }

    /**
     * Sends the statements of the selected members through one channel; 409 {@code statement-has-pending} when
     * entries are pending review and {@code sendAnyway} is false (RF-79b, RF-91).
     *
     * @param request batch
     * @return one outcome per member
     */
    @PostMapping
    public List<DeliveryOutcomeModel> sendStatements(@Valid @RequestBody SendStatementsRequest request) {
        return send.send(request.toSendRequest()).stream().map(DeliveryOutcomeModel::new).toList();
    }

    /**
     * Lists the delivery history, newest first, with the outdated flag (RF-92, RF-93).
     *
     * @param billingPeriod period, optional
     * @param member        member, optional
     * @return history
     */
    @GetMapping
    public CollectionModel<DeliveryModel> statementDeliveries(@RequestParam(required = false) String billingPeriod,
            @RequestParam(required = false) Long member) {
        List<DeliveryModel> rows = deliveries.history(billingPeriod == null ? null : BillingPeriod.parse(billingPeriod), member)
                .stream().map(StatementDeliveryController::toModel).toList();
        return CollectionModel.of(rows, Link.of("/api/statement-deliveries").withSelfRel());
    }

    /**
     * Sends a past delivery again (RF-92).
     *
     * @param id delivery
     * @return the new outcome
     */
    @PostMapping("/{id}/resend")
    public DeliveryOutcomeModel resendStatement(@PathVariable long id) {
        return new DeliveryOutcomeModel(send.resend(id));
    }

    /**
     * Marks a WhatsApp or clipboard delivery as sent (SOL-03).
     *
     * @param id delivery
     * @return the delivery
     */
    @PostMapping("/{id}/sent")
    public DeliveryModel markDeliverySent(@PathVariable long id) {
        return toModel(new HistoryRow(deliveries.markSent(id), false));
    }

    private static DeliveryModel toModel(HistoryRow row) {
        DeliveryModel model = new DeliveryModel(row);
        String self = "/api/statement-deliveries/" + model.id;
        model.add(Link.of(self + "/resend", Relations.RESEND));
        if (model.status == DeliveryStatus.AWAITING_USER) {
            model.add(Link.of(self + "/sent", Relations.MARK_SENT));
        }
        model.add(Link.of("/api/members/" + model.memberId, "member"));
        return model;
    }

    /**
     * Adds {@code statement-deliveries} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor statementDeliveriesRootLink() {
        return () -> Link.of("/api/statement-deliveries", "statement-deliveries");
    }
}

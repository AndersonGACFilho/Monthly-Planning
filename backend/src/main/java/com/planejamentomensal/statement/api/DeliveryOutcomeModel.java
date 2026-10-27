package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.application.DeliveryOutcome;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import org.springframework.hateoas.RepresentationModel;

/**
 * Result of one member in a batch delivery (RF-91).
 *
 * @since 1.0
 */
public class DeliveryOutcomeModel extends RepresentationModel<DeliveryOutcomeModel> {

    /** Member. */
    public final long memberId;
    /** History record. */
    public final long deliveryId;
    /** Result. */
    public final DeliveryStatus status;
    /** Error message or saved file. */
    public final String detail;
    /** Link the interface opens to finish (WhatsApp). */
    public final String action;

    /**
     * Copies an outcome.
     *
     * @param o outcome
     */
    public DeliveryOutcomeModel(DeliveryOutcome o) {
        this.memberId = o.memberId();
        this.deliveryId = o.deliveryId();
        this.status = o.status();
        this.detail = o.detail();
        this.action = o.action();
    }
}

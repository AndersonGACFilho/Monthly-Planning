package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.application.StatementDeliveries.HistoryRow;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import com.planejamentomensal.statement.domain.StatementDelivery;
import java.time.Instant;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * One row of the delivery history (RF-92, RF-93, I18N-12).
 *
 * @since 1.0
 */
@Relation(itemRelation = "delivery", collectionRelation = "deliveries")
public class DeliveryModel extends RepresentationModel<DeliveryModel> {

    /** Id. */
    public final long id;
    /** Member. */
    public final long memberId;
    /** Billing period. */
    public final String billingPeriod;
    /** Channel. */
    public final DeliveryChannelType channel;
    /** Status. */
    public final DeliveryStatus status;
    /** Time. */
    public final Instant createdAt;
    /** Template. */
    public final long templateId;
    /** Template version. */
    public final int templateVersion;
    /** Language used. */
    public final String language;
    /** Error message, link or file. */
    public final String detail;
    /** {@code true} when the statement changed after this delivery (RF-93). */
    public final boolean outdated;

    /**
     * Copies a history row.
     *
     * @param row history row
     */
    public DeliveryModel(HistoryRow row) {
        StatementDelivery d = row.delivery();
        this.id = d.id();
        this.memberId = d.memberId();
        this.billingPeriod = d.billingPeriod().toString();
        this.channel = d.channel();
        this.status = d.status();
        this.createdAt = d.createdAt();
        this.templateId = d.templateId();
        this.templateVersion = d.templateVersion();
        this.language = d.language();
        this.detail = d.detail();
        this.outdated = row.outdated();
    }
}

package com.planejamentomensal.statement.domain;

import com.planejamentomensal.shared.domain.BillingPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One statement delivery in the history: member, period, channel, status, template version, language and
 * a fingerprint of the content to tell when it became outdated (RF-92, RF-93, TPL-06, I18N-12).
 *
 * @since 1.0
 */
@Entity
@Table(name = "statement_delivery")
public class StatementDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private long memberId;

    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryChannelType channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "template_id", nullable = false)
    private long templateId;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String fingerprint;

    @Column(name = "show_categories", nullable = false)
    private boolean showCategories;

    @Column(name = "use_alias", nullable = false)
    private boolean useAlias;

    @Column(name = "include_future", nullable = false)
    private boolean includeFuture;

    private String detail;

    /**
     * For JPA.
     */
    protected StatementDelivery() {
    }

    /**
     * Records a delivery.
     *
     * @param memberId        member
     * @param billingPeriod   period
     * @param channel         channel
     * @param status          result
     * @param createdAt       time
     * @param templateId      template
     * @param templateVersion template version
     * @param language        language used
     * @param fingerprint     fingerprint of the statement content
     * @param options         export options
     * @param detail          error message, link or file path; may be {@code null}
     */
    public StatementDelivery(long memberId, BillingPeriod billingPeriod, DeliveryChannelType channel, DeliveryStatus status,
            Instant createdAt, long templateId, int templateVersion, String language, String fingerprint,
            StatementOptions options, String detail) {
        this.memberId = memberId;
        this.billingPeriod = billingPeriod;
        this.channel = channel;
        this.status = status;
        this.createdAt = createdAt;
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.language = language;
        this.fingerprint = fingerprint;
        this.showCategories = options.showCategories();
        this.useAlias = options.useAlias();
        this.includeFuture = options.includeFuture();
        this.detail = detail;
    }

    /**
     * The user confirms a delivery the app could not finish alone (SOL-03).
     *
     * @throws IllegalStateException unless the delivery is awaiting the user
     */
    public void markSent() {
        if (status != DeliveryStatus.AWAITING_USER) {
            throw new IllegalStateException("Only deliveries awaiting the user can be marked as sent");
        }
        this.status = DeliveryStatus.SENT;
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the member.
     *
     * @return member id
     */
    public long memberId() {
        return memberId;
    }

    /**
     * Returns the period.
     *
     * @return billing period
     */
    public BillingPeriod billingPeriod() {
        return billingPeriod;
    }

    /**
     * Returns the channel.
     *
     * @return channel
     */
    public DeliveryChannelType channel() {
        return channel;
    }

    /**
     * Returns the status.
     *
     * @return status
     */
    public DeliveryStatus status() {
        return status;
    }

    /**
     * Returns the time.
     *
     * @return instant
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * Returns the template.
     *
     * @return template id
     */
    public long templateId() {
        return templateId;
    }

    /**
     * Returns the template version.
     *
     * @return version number
     */
    public int templateVersion() {
        return templateVersion;
    }

    /**
     * Returns the language used.
     *
     * @return language tag
     */
    public String language() {
        return language;
    }

    /**
     * Returns the content fingerprint.
     *
     * @return fingerprint
     */
    public String fingerprint() {
        return fingerprint;
    }

    /**
     * Returns the export options.
     *
     * @return options
     */
    public StatementOptions options() {
        return new StatementOptions(showCategories, useAlias, includeFuture);
    }

    /**
     * Returns the detail.
     *
     * @return error message, link or file path, or {@code null}
     */
    public String detail() {
        return detail;
    }
}

package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.statement.domain.DeliveryStatus;
import com.planejamentomensal.statement.domain.StatementDelivery;
import com.planejamentomensal.statement.domain.StatementFingerprint;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Delivery history with the outdated flag (RF-92, RF-93, I18N-12) and the manual confirmation of deliveries that
 * waited for the user (SOL-03).
 *
 * @since 1.0
 */
@Service
@Transactional
public class StatementDeliveries {

    private final StatementDeliveryRepository deliveries;
    private final MemberStatementBuilder builder;

    /**
     * Creates the service.
     *
     * @param deliveries history storage
     * @param builder    statement builder, to compare fingerprints
     */
    public StatementDeliveries(StatementDeliveryRepository deliveries, MemberStatementBuilder builder) {
        this.deliveries = deliveries;
        this.builder = builder;
    }

    /**
     * A history row.
     *
     * @param delivery delivery
     * @param outdated {@code true} when the statement changed after it was sent (RF-93)
     */
    public record HistoryRow(StatementDelivery delivery, boolean outdated) {
    }

    /**
     * Lists the history, newest first, optionally for one period and member.
     *
     * @param period   billing period, or {@code null}
     * @param memberId member, or {@code null}
     * @return rows
     */
    public List<HistoryRow> history(BillingPeriod period, Long memberId) {
        Map<String, String> current = new HashMap<>();
        return deliveries.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(d -> period == null || d.billingPeriod().equals(period))
                .filter(d -> memberId == null || d.memberId() == memberId)
                .map(d -> new HistoryRow(d, !Objects.equals(d.fingerprint(), current.computeIfAbsent(
                        d.memberId() + "|" + d.billingPeriod() + "|" + d.options(),
                        k -> StatementFingerprint.of(builder.build(d.memberId(), d.billingPeriod(), d.options()))))))
                .toList();
    }

    /**
     * The user confirms a WhatsApp or clipboard delivery (SOL-03).
     *
     * @param id delivery
     * @return the delivery
     * @throws BusinessRuleException with code {@code delivery-not-awaiting} unless it awaits the user
     */
    public StatementDelivery markSent(long id) {
        StatementDelivery delivery = deliveries.findById(id).orElseThrow(() -> new NotFoundException("statement-delivery", id));
        if (delivery.status() != DeliveryStatus.AWAITING_USER) {
            throw new BusinessRuleException("delivery-not-awaiting", id);
        }
        delivery.markSent();
        return deliveries.saveAndFlush(delivery);
    }
}

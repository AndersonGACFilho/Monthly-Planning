package com.planejamentomensal.statement.application;

import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.statement.domain.DeliveryChannelType;
import com.planejamentomensal.statement.domain.StatementDelivery;
import com.planejamentomensal.statement.domain.StatementFingerprint;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends statements: build → render → generate → send → record the history, one member at a time; a failure of one
 * member never stops the others (IMP-37, RF-91, RF-92).
 *
 * @since 1.0
 */
@Service
@Transactional
public class SendStatementUseCase {

    private final StatementService statements;
    private final StatementDeliveryRepository deliveries;
    private final Map<DeliveryChannelType, DeliveryChannel> channels = new EnumMap<>(DeliveryChannelType.class);
    private final Clock clock;

    /**
     * Creates the use case.
     *
     * @param statements statement preparation
     * @param deliveries history storage
     * @param channels   delivery channels
     * @param clock      clock
     */
    public SendStatementUseCase(StatementService statements, StatementDeliveryRepository deliveries,
            List<DeliveryChannel> channels, Clock clock) {
        this.statements = statements;
        this.deliveries = deliveries;
        channels.forEach(c -> this.channels.put(c.type(), c));
        this.clock = clock;
    }

    /**
     * Sends the statements of the selected members.
     *
     * @param request batch
     * @return one outcome per member, in the requested order
     * @throws BusinessRuleException with code {@code statement-has-pending} when entries are pending review and the
     *                               user did not choose to send anyway (RF-79b)
     */
    public List<DeliveryOutcome> send(SendRequest request) {
        List<PreparedStatement> prepared = request.memberIds().stream()
                .map(id -> statements.prepare(new StatementRequest(id, request.period(), request.options(),
                        request.language(), request.templateId(), request.note())))
                .toList();
        int pending = prepared.stream().mapToInt(p -> p.statement().pendingItems().size()).sum();
        if (pending > 0 && !request.sendAnyway()) {
            throw new BusinessRuleException("statement-has-pending", pending);
        }
        DeliveryChannel channel = channels.get(request.channel());
        List<DeliveryOutcome> outcomes = new ArrayList<>();
        for (PreparedStatement p : prepared) {
            DeliveryResult result;
            try {
                byte[] pdf = statements.pdf(p);
                result = channel.deliver(new DeliveryPackage(p.statement(), p.rendered(), p.email(), p.phone(), pdf,
                        () -> statements.png(p)));
            } catch (RuntimeException e) {
                result = DeliveryResult.failed(e.getMessage());
            }
            StatementDelivery delivery = deliveries.saveAndFlush(new StatementDelivery(p.statement().memberId(),
                    request.period(), request.channel(), result.status(), clock.instant(), p.template().templateId(),
                    p.template().version(), p.rendered().language(), StatementFingerprint.of(p.statement()),
                    request.options(), result.detail()));
            outcomes.add(new DeliveryOutcome(p.statement().memberId(), delivery.id(), result.status(), result.detail(),
                    result.action()));
        }
        return outcomes;
    }

    /**
     * Sends a past delivery again with the same channel, options and language (RF-92).
     *
     * @param deliveryId history record
     * @return the outcome
     */
    public DeliveryOutcome resend(long deliveryId) {
        StatementDelivery old = deliveries.findById(deliveryId)
                .orElseThrow(() -> new NotFoundException("statement-delivery", deliveryId));
        return send(new SendRequest(old.billingPeriod(), List.of(old.memberId()), old.channel(), old.options(),
                old.language(), old.templateId(), null, true)).getFirst();
    }
}

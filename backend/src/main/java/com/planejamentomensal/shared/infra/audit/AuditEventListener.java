package com.planejamentomensal.shared.infra.audit;

import com.planejamentomensal.shared.domain.Auditable;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an audit row (before, after, when) for every change of an {@link Auditable} entity, in the same
 * transaction (RNF-05). One listener for every entity instead of code in each service (SPEC section 13).
 *
 * @since 1.0
 */
@Component
public class AuditEventListener implements PostUpdateEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final JsonMapper json = JsonMapper.builder().build();

    /**
     * Creates the listener.
     *
     * @param entityManagerFactory JPA factory, to register with Hibernate
     */
    public AuditEventListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Registers the listener for post-update events.
     */
    @PostConstruct
    void register() {
        entityManagerFactory.unwrap(SessionFactoryImplementor.class).getServiceRegistry()
                .getService(EventListenerRegistry.class).appendListeners(EventType.POST_UPDATE, this);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (!(event.getEntity() instanceof Auditable auditable) || event.getOldState() == null) {
            return;
        }
        String[] names = event.getPersister().getPropertyNames();
        Object previousStatus = valueOf(names, event.getOldState(), "status");
        if (!auditable.audits(previousStatus)) {
            return;
        }
        Map<String, Object> before = new LinkedHashMap<>();
        Map<String, Object> after = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            Object old = simple(event.getOldState()[i]);
            Object now = simple(event.getState()[i]);
            if (!java.util.Objects.equals(old, now) && !names[i].equals("version")) {
                before.put(names[i], old);
                after.put(names[i], now);
            }
        }
        if (after.isEmpty()) {
            return;
        }
        String beforeJson = json.writeValueAsString(before);
        String afterJson = json.writeValueAsString(after);
        Object id = event.getId();
        event.getSession().doWork(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO audit_entry (entity_type, entity_id, changed_at, before_json, after_json) VALUES (?, ?, ?, ?, ?)")) {
                insert.setString(1, auditable.auditType());
                insert.setLong(2, ((Number) id).longValue());
                insert.setString(3, Instant.now().toString());
                insert.setString(4, beforeJson);
                insert.setString(5, afterJson);
                insert.executeUpdate();
            }
        });
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private static Object valueOf(String[] names, Object[] state, String property) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].equals(property)) {
                return state[i];
            }
        }
        return null;
    }

    private static Object simple(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof java.util.Collection<?> collection) {
            return collection.size();
        }
        try {
            var id = value.getClass().getMethod("id");
            return id.invoke(value);
        } catch (ReflectiveOperationException e) {
            return value.toString();
        }
    }
}

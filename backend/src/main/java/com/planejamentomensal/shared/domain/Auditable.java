package com.planejamentomensal.shared.domain;

/**
 * Marks an entity whose changes are recorded in the audit log (RNF-05).
 *
 * @since 1.0
 */
public interface Auditable {

    /**
     * Name of the entity in the audit log.
     *
     * @return type name, such as {@code bill-entry}
     */
    String auditType();

    /**
     * Tells whether a change from the given previous state must be audited.
     *
     * @param previousStatus value of the {@code status} property before the change, or {@code null}
     * @return {@code true} to record the change
     */
    default boolean audits(Object previousStatus) {
        return true;
    }
}

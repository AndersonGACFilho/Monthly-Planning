package com.planejamentomensal.shared.domain;

/**
 * The client edited an outdated version of a resource (HTTP 412, API-04).
 *
 * @since 1.0
 */
public class StaleVersionException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param expected version sent by the client in {@code If-Match}
     * @param current  version currently stored
     */
    public StaleVersionException(long expected, long current) {
        super("stale-version", expected, current);
    }
}

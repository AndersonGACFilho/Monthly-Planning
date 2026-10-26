package com.planejamentomensal.shared.domain;

/**
 * A requested resource does not exist (HTTP 404, API-03).
 *
 * @since 1.0
 */
public class NotFoundException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param resource resource name in kebab-case, such as {@code "member"}
     * @param id       identifier that was not found
     */
    public NotFoundException(String resource, Object id) {
        super("not-found", resource, id);
    }
}

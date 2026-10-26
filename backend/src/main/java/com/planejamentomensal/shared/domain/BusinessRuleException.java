package com.planejamentomensal.shared.domain;

/**
 * An operation conflicts with a business rule, such as undoing a batch twice (HTTP 409, API-03).
 *
 * @since 1.0
 */
public class BusinessRuleException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param code      stable error code in kebab-case
     * @param arguments values inserted in the localized message
     */
    public BusinessRuleException(String code, Object... arguments) {
        super(code, arguments);
    }
}

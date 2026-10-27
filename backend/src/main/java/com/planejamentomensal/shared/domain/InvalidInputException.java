package com.planejamentomensal.shared.domain;

/**
 * Input that cannot be processed, such as a file that is not an Itaú bill (HTTP 400, API-03).
 *
 * @since 1.0
 */
public class InvalidInputException extends DomainException {

    /**
     * Creates the exception.
     *
     * @param code      stable error code in kebab-case
     * @param arguments values inserted in the localized message
     */
    public InvalidInputException(String code, Object... arguments) {
        super(code, arguments);
    }
}

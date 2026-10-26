package com.planejamentomensal.shared.domain;

import java.util.List;
import java.util.Objects;

/**
 * Base class of business errors raised by the domain and application layers.
 *
 * <p>Carries a stable {@linkplain #code() code} and arguments instead of a user message: the API turns
 * the code into a localized RFC 9457 problem (API-03, I18N-06).</p>
 *
 * @since 1.0
 */
public abstract class DomainException extends RuntimeException {

    /** Stable error code in kebab-case. */
    private final String code;
    /** Values inserted in the localized message, not serialized. */
    private final transient List<Object> arguments;

    /**
     * Creates the exception.
     *
     * @param code      stable error code in kebab-case, such as {@code "member-has-entries"}
     * @param arguments values inserted in the localized message, in order
     */
    protected DomainException(String code, Object... arguments) {
        super(code);
        this.code = Objects.requireNonNull(code, "code");
        this.arguments = List.of(arguments);
    }

    /**
     * Returns the stable error code.
     *
     * @return error code, used as message key {@code error.<code>.title} / {@code error.<code>.detail}
     */
    public String code() {
        return code;
    }

    /**
     * Returns the values inserted in the localized message.
     *
     * @return message arguments, possibly empty
     */
    public List<Object> arguments() {
        return arguments;
    }
}

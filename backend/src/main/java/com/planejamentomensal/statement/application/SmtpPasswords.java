package com.planejamentomensal.statement.application;

import java.util.Optional;

/**
 * Keeps the SMTP password encrypted in the database (RF-86, Q9).
 *
 * @since 1.0
 */
public interface SmtpPasswords {

    /**
     * Stores the password, or removes it when blank.
     *
     * @param password password, may be {@code null}
     */
    void store(String password);

    /**
     * Reads the password.
     *
     * @return the password, or empty when none was stored
     */
    Optional<String> read();
}

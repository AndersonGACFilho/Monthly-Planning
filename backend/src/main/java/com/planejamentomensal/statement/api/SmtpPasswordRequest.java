package com.planejamentomensal.statement.api;

import jakarta.validation.constraints.Size;

/**
 * SMTP password; blank removes it (RF-86).
 *
 * @param password password
 * @since 1.0
 */
public record SmtpPasswordRequest(@Size(max = 255) String password) {
}

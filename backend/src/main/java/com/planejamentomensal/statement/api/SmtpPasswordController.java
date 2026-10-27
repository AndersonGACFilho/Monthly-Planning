package com.planejamentomensal.statement.api;

import com.planejamentomensal.statement.application.SmtpPasswords;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Write-only SMTP password: it is stored encrypted and never returned (RF-86, Q9).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/settings/smtp-password")
public class SmtpPasswordController {

    private final SmtpPasswords passwords;

    /**
     * Creates the controller.
     *
     * @param passwords password store
     */
    public SmtpPasswordController(SmtpPasswords passwords) {
        this.passwords = passwords;
    }

    /**
     * Tells whether a password is stored.
     *
     * @return {@code {"configured": true|false}}
     */
    @GetMapping
    public Map<String, Boolean> smtpPassword() {
        return Map.of("configured", passwords.read().isPresent());
    }

    /**
     * Stores or removes the password.
     *
     * @param request password; blank removes it
     * @return {@code {"configured": true|false}}
     */
    @PutMapping
    public Map<String, Boolean> changeSmtpPassword(@Valid @RequestBody SmtpPasswordRequest request) {
        passwords.store(request.password());
        return smtpPassword();
    }
}

package com.planejamentomensal.member.api;

import com.planejamentomensal.member.domain.MemberDetails;
import com.planejamentomensal.member.domain.MemberKind;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data to create or replace a member (RF-01, RF-80).
 *
 * @param code              short code: 1 to 4 upper-case letters or digits
 * @param name              name
 * @param kind              MAIN or THIRD_PARTY
 * @param color             colour as {@code #RRGGBB}
 * @param email             e-mail, optional
 * @param whatsappPhone     phone with country code such as {@code +5551999990000}, optional
 * @param pixKey            PIX key, optional
 * @param preferredLanguage {@code pt-BR} or {@code en-US}
 * @since 1.0
 */
public record MemberRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9]{1,4}") String code,
        @NotBlank @Size(max = 60) String name,
        @NotNull MemberKind kind,
        @NotBlank @Pattern(regexp = "#[0-9a-fA-F]{6}") String color,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "\\+[1-9][0-9]{7,14}") String whatsappPhone,
        @Size(max = 77) String pixKey,
        @NotBlank @Pattern(regexp = "pt-BR|en-US") String preferredLanguage) {

    /**
     * Converts to domain data, turning blank optional fields into {@code null}.
     *
     * @return member details
     */
    public MemberDetails toDetails() {
        return new MemberDetails(code, name.strip(), kind, color.toUpperCase(), blankToNull(email),
                blankToNull(whatsappPhone), blankToNull(pixKey), preferredLanguage);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}

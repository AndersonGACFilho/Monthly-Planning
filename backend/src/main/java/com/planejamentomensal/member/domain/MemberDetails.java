package com.planejamentomensal.member.domain;

/**
 * Editable data of a {@link Member}.
 *
 * @param code              short code, such as {@code AB}
 * @param name              name
 * @param kind              MAIN or THIRD_PARTY
 * @param color             colour as {@code #RRGGBB}
 * @param email             e-mail, or {@code null}
 * @param whatsappPhone     phone with country code, or {@code null}
 * @param pixKey            PIX key, or {@code null}
 * @param preferredLanguage {@code pt-BR} or {@code en-US}
 * @since 1.0
 */
public record MemberDetails(String code, String name, MemberKind kind, String color, String email,
        String whatsappPhone, String pixKey, String preferredLanguage) {
}

package com.planejamentomensal.member.application;

import com.planejamentomensal.member.domain.Member;

/**
 * Read model of a member for other modules, which may not see the member domain (SOL-06).
 *
 * @param id                id
 * @param code              short code
 * @param name              name
 * @param main              {@code true} for a MAIN member, {@code false} for a third party
 * @param color             colour
 * @param active            active flag
 * @param email             e-mail, or {@code null}
 * @param whatsappPhone     WhatsApp phone, or {@code null}
 * @param pixKey            PIX key, or {@code null}
 * @param preferredLanguage statement language
 * @since 1.0
 */
public record MemberView(long id, String code, String name, boolean main, String color, boolean active, String email,
        String whatsappPhone, String pixKey, String preferredLanguage) {

    /**
     * Builds the view of a member.
     *
     * @param member member
     * @return its view
     */
    public static MemberView of(Member member) {
        return new MemberView(member.id(), member.code(), member.name(),
                member.kind() == com.planejamentomensal.member.domain.MemberKind.MAIN, member.color(), member.active(),
                member.email(), member.whatsappPhone(), member.pixKey(), member.preferredLanguage());
    }
}

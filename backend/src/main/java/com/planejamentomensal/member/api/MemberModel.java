package com.planejamentomensal.member.api;

import com.planejamentomensal.member.domain.Member;
import com.planejamentomensal.member.domain.MemberKind;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a member.
 *
 * @since 1.0
 */
@Relation(itemRelation = "member", collectionRelation = "members")
public class MemberModel extends RepresentationModel<MemberModel> {

    /** Id. */
    public final long id;
    /** Short code. */
    public final String code;
    /** Name. */
    public final String name;
    /** Kind. */
    public final MemberKind kind;
    /** Colour. */
    public final String color;
    /** Active flag. */
    public final boolean active;
    /** E-mail. */
    public final String email;
    /** WhatsApp phone. */
    public final String whatsappPhone;
    /** PIX key. */
    public final String pixKey;
    /** Statement language. */
    public final String preferredLanguage;

    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies a member.
     *
     * @param member member
     */
    public MemberModel(Member member) {
        this.id = member.id();
        this.code = member.code();
        this.name = member.name();
        this.kind = member.kind();
        this.color = member.color();
        this.active = member.active();
        this.email = member.email();
        this.whatsappPhone = member.whatsappPhone();
        this.pixKey = member.pixKey();
        this.preferredLanguage = member.preferredLanguage();
        this.version = member.version();
    }
}

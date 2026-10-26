package com.planejamentomensal.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;

/**
 * A member (conta / cota): a person or cost centre that owns incomes, expenses and card entries.
 *
 * <p>Implements RF-01 (code, name, colour, active flag; inactivate instead of delete when there is
 * history), RF-80 (contact data and preferred language for statements) and XLS-04 (kind).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberKind kind;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private boolean active = true;

    private String email;

    @Column(name = "whatsapp_phone")
    private String whatsappPhone;

    @Column(name = "pix_key")
    private String pixKey;

    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Member() {
    }

    /**
     * Creates an active member.
     *
     * @param details editable data of the member
     */
    public Member(MemberDetails details) {
        apply(details);
    }

    /**
     * Replaces the editable data.
     *
     * @param details new data
     */
    public void update(MemberDetails details) {
        apply(details);
    }

    private void apply(MemberDetails details) {
        Objects.requireNonNull(details, "details");
        this.code = details.code();
        this.name = details.name();
        this.kind = details.kind();
        this.color = details.color();
        this.email = details.email();
        this.whatsappPhone = details.whatsappPhone();
        this.pixKey = details.pixKey();
        this.preferredLanguage = details.preferredLanguage();
    }

    /**
     * Makes the member active or inactive (RF-01).
     *
     * @param newActive {@code true} to activate
     */
    public void setActive(boolean newActive) {
        this.active = newActive;
    }

    /**
     * Returns the id.
     *
     * @return id, {@code null} before saving
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the short code, such as {@code AB}.
     *
     * @return code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the kind.
     *
     * @return MAIN or THIRD_PARTY
     */
    public MemberKind kind() {
        return kind;
    }

    /**
     * Returns the colour.
     *
     * @return {@code #RRGGBB}
     */
    public String color() {
        return color;
    }

    /**
     * Tells whether the member is active.
     *
     * @return {@code true} if active
     */
    public boolean active() {
        return active;
    }

    /**
     * Returns the e-mail.
     *
     * @return e-mail, or {@code null}
     */
    public String email() {
        return email;
    }

    /**
     * Returns the WhatsApp phone with country code.
     *
     * @return phone such as {@code +5551999990000}, or {@code null}
     */
    public String whatsappPhone() {
        return whatsappPhone;
    }

    /**
     * Returns the PIX key for payments.
     *
     * @return PIX key, or {@code null}
     */
    public String pixKey() {
        return pixKey;
    }

    /**
     * Returns the language of statements sent to this member (I18N-08).
     *
     * @return {@code pt-BR} or {@code en-US}
     */
    public String preferredLanguage() {
        return preferredLanguage;
    }

    /**
     * Returns the optimistic lock version (API-04).
     *
     * @return version
     */
    public long version() {
        return version;
    }
}

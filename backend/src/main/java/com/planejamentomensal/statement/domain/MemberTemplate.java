package com.planejamentomensal.statement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Template chosen for one member instead of the default (TPL-04).
 *
 * @since 1.0
 */
@Entity
@Table(name = "member_template")
public class MemberTemplate {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "template_id", nullable = false)
    private long templateId;

    /**
     * For JPA.
     */
    protected MemberTemplate() {
    }

    /**
     * Assigns a template to a member.
     *
     * @param memberId   member
     * @param templateId template
     */
    public MemberTemplate(long memberId, long templateId) {
        this.memberId = memberId;
        this.templateId = templateId;
    }

    /**
     * Changes the template.
     *
     * @param newTemplateId template
     */
    public void use(long newTemplateId) {
        this.templateId = newTemplateId;
    }

    /**
     * Returns the member.
     *
     * @return member id
     */
    public long memberId() {
        return memberId;
    }

    /**
     * Returns the template.
     *
     * @return template id
     */
    public long templateId() {
        return templateId;
    }
}

package com.planejamentomensal.statement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Frozen settings of a template at one version, so a sent statement can be reproduced (TPL-06).
 *
 * @since 1.0
 */
@Entity
@Table(name = "template_version")
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private long templateId;

    @Column(nullable = false)
    private int number;

    @Column(name = "settings_json", nullable = false)
    private String settingsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * For JPA.
     */
    protected TemplateVersion() {
    }

    /**
     * Freezes a version.
     *
     * @param templateId   template
     * @param number       version number
     * @param settingsJson settings as JSON
     * @param createdAt    time
     */
    public TemplateVersion(long templateId, int number, String settingsJson, Instant createdAt) {
        this.templateId = templateId;
        this.number = number;
        this.settingsJson = settingsJson;
        this.createdAt = createdAt;
    }

    /**
     * Returns the id.
     *
     * @return id
     */
    public Long id() {
        return id;
    }

    /**
     * Returns the template.
     *
     * @return template id
     */
    public long templateId() {
        return templateId;
    }

    /**
     * Returns the version number.
     *
     * @return number
     */
    public int number() {
        return number;
    }

    /**
     * Returns the settings as JSON.
     *
     * @return JSON
     */
    public String settingsJson() {
        return settingsJson;
    }

    /**
     * Returns the creation time.
     *
     * @return instant
     */
    public Instant createdAt() {
        return createdAt;
    }
}

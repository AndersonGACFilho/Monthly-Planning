package com.planejamentomensal.statement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A saved statement template; its settings are kept as JSON and every change makes a new
 * {@link TemplateVersion} (TPL-04, TPL-06).
 *
 * @since 1.0
 */
@Entity
@Table(name = "template")
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_template", nullable = false)
    private boolean defaultTemplate;

    @Column(name = "settings_json", nullable = false)
    private String settingsJson;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Template() {
    }

    /**
     * Creates a template at version 1.
     *
     * @param name         name
     * @param settingsJson settings as JSON
     * @param now          creation time
     */
    public Template(String name, String settingsJson, Instant now) {
        this.name = name;
        this.settingsJson = settingsJson;
        this.currentVersion = 1;
        this.updatedAt = now;
    }

    /**
     * Changes name and settings and moves to the next version (TPL-06).
     *
     * @param newName     name
     * @param newSettings settings as JSON
     * @param now         change time
     * @return {@code true} if the settings changed and a new version is due
     */
    public boolean change(String newName, String newSettings, Instant now) {
        this.name = newName;
        boolean changed = !newSettings.equals(settingsJson);
        if (changed) {
            this.settingsJson = newSettings;
            this.currentVersion++;
        }
        this.updatedAt = now;
        return changed;
    }

    /**
     * Marks or unmarks this template as the default one (TPL-04).
     *
     * @param value new flag
     */
    public void setDefault(boolean value) {
        this.defaultTemplate = value;
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
     * Returns the name.
     *
     * @return name
     */
    public String name() {
        return name;
    }

    /**
     * Tells whether this is the default template.
     *
     * @return default flag
     */
    public boolean isDefault() {
        return defaultTemplate;
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
     * Returns the current version number.
     *
     * @return version number, from 1
     */
    public int currentVersion() {
        return currentVersion;
    }

    /**
     * Returns the time of the last change.
     *
     * @return instant
     */
    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * Returns the optimistic-lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}

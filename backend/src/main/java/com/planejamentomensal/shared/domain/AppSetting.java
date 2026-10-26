package com.planejamentomensal.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Objects;

/**
 * One user setting stored as text under a key (IMP-10, Q16).
 *
 * @since 1.0
 */
@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(name = "setting_key")
    private String key;

    @Column(name = "setting_value", nullable = false)
    private String value;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected AppSetting() {
    }

    /**
     * Creates a setting.
     *
     * @param key   setting key
     * @param value value as text
     */
    public AppSetting(String key, String value) {
        this.key = Objects.requireNonNull(key, "key");
        this.value = Objects.requireNonNull(value, "value");
    }

    /**
     * Returns the key.
     *
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the value as text.
     *
     * @return value
     */
    public String value() {
        return value;
    }

    /**
     * Changes the value.
     *
     * @param newValue value as text
     */
    public void change(String newValue) {
        this.value = Objects.requireNonNull(newValue, "value");
    }
}

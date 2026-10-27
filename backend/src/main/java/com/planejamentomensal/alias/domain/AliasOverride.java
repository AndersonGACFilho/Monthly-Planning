package com.planejamentomensal.alias.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Name given to one entry only, without changing the alias of its description (RF-48).
 *
 * @since 1.0
 */
@Entity
@Table(name = "alias_override")
public class AliasOverride {

    @Id
    @Column(name = "bill_entry_id")
    private Long billEntryId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /**
     * For JPA.
     */
    protected AliasOverride() {
    }

    /**
     * Creates an override.
     *
     * @param billEntryId entry
     * @param displayName name for that entry
     */
    public AliasOverride(long billEntryId, String displayName) {
        this.billEntryId = billEntryId;
        rename(displayName);
    }

    /**
     * Replaces the name.
     *
     * @param newName new name
     * @throws IllegalArgumentException if the name is blank
     */
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("An override needs a name");
        }
        this.displayName = newName.strip();
    }

    /**
     * Returns the entry.
     *
     * @return entry id
     */
    public Long billEntryId() {
        return billEntryId;
    }

    /**
     * Returns the name.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }
}

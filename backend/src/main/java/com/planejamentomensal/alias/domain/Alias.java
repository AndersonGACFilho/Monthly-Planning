package com.planejamentomensal.alias.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Friendly name given to one or more original card descriptions (RF-46 to RF-49, RN-24).
 *
 * <p>The alias points to normalized original descriptions (patterns), never to another alias: renaming
 * again replaces the name (RF-47). One alias groups several patterns, such as the PDF and XLSX texts of
 * the same store (RN-24). It may carry a default category for future imports (RF-49).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "alias")
public class Alias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "default_category_id")
    private Long defaultCategoryId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alias_pattern", joinColumns = @JoinColumn(name = "alias_id"))
    @Column(name = "normalized_description", nullable = false, unique = true)
    private Set<String> patterns = new LinkedHashSet<>();

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Alias() {
    }

    /**
     * Creates an alias for one normalized description.
     *
     * @param displayName       name shown instead of the original
     * @param pattern           normalized original description (RN-15)
     * @param defaultCategoryId category for future imports, or {@code null}
     */
    public Alias(String displayName, String pattern, Long defaultCategoryId) {
        rename(displayName);
        this.patterns.add(Objects.requireNonNull(pattern, "pattern"));
        this.defaultCategoryId = defaultCategoryId;
    }

    /**
     * Replaces the name; patterns stay the same (RF-47).
     *
     * @param newName new name
     * @throws IllegalArgumentException if the name is blank
     */
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("An alias needs a name");
        }
        this.displayName = newName.strip();
    }

    /**
     * Sets the default category (RF-49).
     *
     * @param categoryId category, or {@code null} for none
     */
    public void defaultCategory(Long categoryId) {
        this.defaultCategoryId = categoryId;
    }

    /**
     * Adds a normalized description of the same store (RN-24).
     *
     * @param pattern normalized description
     */
    public void addPattern(String pattern) {
        patterns.add(pattern);
    }

    /**
     * Removes a normalized description (RF-50).
     *
     * @param pattern normalized description
     * @return {@code true} if the alias has no pattern left and should be deleted
     */
    public boolean removePattern(String pattern) {
        patterns.remove(pattern);
        return patterns.isEmpty();
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
     * Returns the name shown instead of the original description.
     *
     * @return display name
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the default category.
     *
     * @return category id, or {@code null}
     */
    public Long defaultCategoryId() {
        return defaultCategoryId;
    }

    /**
     * Returns the normalized descriptions the alias names.
     *
     * @return patterns
     */
    public Set<String> patterns() {
        return Set.copyOf(patterns);
    }

    /**
     * Returns the optimistic lock version.
     *
     * @return version
     */
    public long version() {
        return version;
    }
}

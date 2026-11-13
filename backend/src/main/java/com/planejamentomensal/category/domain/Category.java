package com.planejamentomensal.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Spending category with colour, icon and optional parent (RF-06).
 *
 * <p>The system category "not classified" receives everything without a category and cannot be deleted
 * (RF-07). Names are user data and are never translated after creation (I18N-07).</p>
 *
 * @since 1.0
 */
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    private String color;

    @Column(nullable = false)
    private String icon;

    @Column(name = "system_category", nullable = false)
    private boolean system;

    @Column(nullable = false)
    private boolean subscription;

    @Version
    private long version;

    /**
     * For JPA.
     */
    protected Category() {
    }

    /**
     * Creates a category.
     *
     * @param details editable data
     * @param system  {@code true} only for "not classified"
     */
    public Category(CategoryDetails details, boolean system) {
        this.system = system;
        update(details);
    }

    /**
     * Changes the editable data; the system flag never changes, and the system category has no parent and is never a
     * subscription.
     *
     * @param details new data
     */
    public void update(CategoryDetails details) {
        this.name = details.name();
        this.parentId = system ? null : details.parentId();
        this.color = details.color();
        this.icon = details.icon();
        this.subscription = !system && details.subscription();
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
     * Returns the parent.
     *
     * @return parent id, or {@code null}
     */
    public Long parentId() {
        return parentId;
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
     * Returns the icon name.
     *
     * @return icon
     */
    public String icon() {
        return icon;
    }

    /**
     * Tells whether this is the "not classified" system category (RF-07).
     *
     * @return system flag
     */
    public boolean system() {
        return system;
    }

    /**
     * Tells whether single card purchases in this category are subscriptions (IMP-65).
     *
     * @return subscription flag
     */
    public boolean subscription() {
        return subscription;
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

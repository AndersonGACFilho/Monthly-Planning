package com.planejamentomensal.category.application;

import com.planejamentomensal.category.domain.Category;

/**
 * Read model of a category for other modules (SOL-06).
 *
 * @param id       id
 * @param name     name
 * @param parentId parent, or {@code null}
 * @param color    colour
 * @param icon     icon
 * @param system   {@code true} for "not classified"
 * @since 1.0
 */
public record CategoryView(long id, String name, Long parentId, String color, String icon, boolean system) {

    /**
     * Builds the view of a category.
     *
     * @param category category
     * @return its view
     */
    public static CategoryView of(Category category) {
        return new CategoryView(category.id(), category.name(), category.parentId(), category.color(), category.icon(),
                category.system());
    }
}

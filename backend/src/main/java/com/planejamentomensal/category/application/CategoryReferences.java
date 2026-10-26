package com.planejamentomensal.category.application;

/**
 * Tells whether something in another module points to a category (SOL-02).
 *
 * @since 1.0
 */
public interface CategoryReferences {

    /**
     * Tells whether the category is used.
     *
     * @param categoryId category id
     * @return {@code true} if deleting it would lose history
     */
    boolean isReferenced(long categoryId);
}

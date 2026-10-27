package com.planejamentomensal.alias.application;

import com.planejamentomensal.category.application.CategoryReferences;
import org.springframework.stereotype.Component;

/**
 * A category used as an alias default cannot be deleted (RF-49).
 *
 * @since 1.0
 */
@Component
public class AliasCategoryReferences implements CategoryReferences {

    private final AliasRepository aliases;

    /**
     * Creates the check.
     *
     * @param aliases alias storage
     */
    public AliasCategoryReferences(AliasRepository aliases) {
        this.aliases = aliases;
    }

    /**
     * Tells whether an alias uses the category.
     *
     * @param categoryId category
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long categoryId) {
        return aliases.existsByDefaultCategoryId(categoryId);
    }
}

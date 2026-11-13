package com.planejamentomensal.category.api;

import com.planejamentomensal.category.domain.CategoryDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data to create or replace a category (RF-06).
 *
 * @param name         name
 * @param parentId     parent category for a subcategory, optional
 * @param color        colour as {@code #RRGGBB}
 * @param icon         icon name in kebab-case
 * @param subscription single card purchases in it are subscriptions (IMP-65); {@code false} when missing
 * @since 1.0
 */
public record CategoryRequest(
        @NotBlank @Size(max = 40) String name,
        Long parentId,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String color,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{1,40}$") String icon,
        boolean subscription) {

    /**
     * Returns the category data, with the name trimmed and the colour in capitals.
     *
     * @return category data
     */
    public CategoryDetails toDetails() {
        return new CategoryDetails(name.strip(), parentId, color.toUpperCase(), icon, subscription);
    }
}

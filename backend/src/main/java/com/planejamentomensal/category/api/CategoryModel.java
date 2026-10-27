package com.planejamentomensal.category.api;

import com.planejamentomensal.category.domain.Category;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * HAL representation of a category.
 *
 * @since 1.0
 */
@Relation(itemRelation = "category", collectionRelation = "categories")
public class CategoryModel extends RepresentationModel<CategoryModel> {

    /** Id. */
    public final long id;
    /** Name. */
    public final String name;
    /** Parent category. */
    public final Long parentId;
    /** Colour. */
    public final String color;
    /** Icon name. */
    public final String icon;
    /** {@code true} for "not classified". */
    public final boolean system;

    /** Version for {@code If-Match} (API-04). */
    public final long version;

    /**
     * Copies a category.
     *
     * @param category category
     */
    public CategoryModel(Category category) {
        this.id = category.id();
        this.name = category.name();
        this.parentId = category.parentId();
        this.color = category.color();
        this.icon = category.icon();
        this.system = category.system();
        this.version = category.version();
    }
}

package com.planejamentomensal.category.api;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.category.domain.Category;
import com.planejamentomensal.shared.api.Relations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

/**
 * Builds the category representation and its links (API-08, API-09, API-11).
 *
 * @since 1.0
 */
@Component
public class CategoryModelAssembler implements RepresentationModelAssembler<Category, CategoryModel> {

    private final CategoryQueries queries;

    /**
     * Creates the assembler.
     *
     * @param queries category queries
     */
    public CategoryModelAssembler(CategoryQueries queries) {
        this.queries = queries;
    }

    /**
     * Converts a category.
     *
     * @param category category
     * @return representation with links
     */
    @Override
    public CategoryModel toModel(Category category) {
        CategoryModel model = new CategoryModel(category);
        Link self = linkTo(methodOn(CategoryController.class).category(category.id())).withSelfRel();
        model.add(self);
        if (category.parentId() != null) {
            model.add(linkTo(methodOn(CategoryController.class).category(category.parentId())).withRel("parent"));
        }
        if (queries.isDeletable(category.id())) {
            model.add(self.withRel(Relations.DELETE));
        }
        return model;
    }
}

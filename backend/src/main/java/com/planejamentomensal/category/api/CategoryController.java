package com.planejamentomensal.category.api;

import com.planejamentomensal.category.application.CategoryQueries;
import com.planejamentomensal.category.application.CategoryRepository;
import com.planejamentomensal.category.application.SaveCategoryUseCase;
import com.planejamentomensal.category.domain.Category;
import com.planejamentomensal.shared.api.ETags;
import com.planejamentomensal.shared.api.RootLinkContributor;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spending categories with colour, icon and optional parent (RF-06, RF-07, I18N-07).
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryRepository categories;
    private final CategoryQueries queries;
    private final SaveCategoryUseCase save;
    private final CategoryModelAssembler assembler;

    /**
     * Creates the controller.
     *
     * @param categories category storage
     * @param queries    category queries
     * @param save       create, edit, seed and delete use case
     * @param assembler  HAL assembler
     */
    public CategoryController(CategoryRepository categories, CategoryQueries queries, SaveCategoryUseCase save,
            CategoryModelAssembler assembler) {
        this.categories = categories;
        this.queries = queries;
        this.save = save;
        this.assembler = assembler;
    }

    /**
     * Lists categories by name; "not classified" is always present.
     *
     * @param pageable  page, size and sort
     * @param resources paging assembler
     * @return one page of categories
     */
    @GetMapping
    public PagedModel<CategoryModel> categories(@PageableDefault(size = 200, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable, PagedResourcesAssembler<Category> resources) {
        queries.unclassifiedId();
        return resources.toModel(categories.findAll(pageable), assembler);
    }

    /**
     * Returns one category.
     *
     * @param id category id
     * @return the category
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryModel> category(@PathVariable long id) {
        Category category = queries.get(id);
        return ETags.ok(assembler.toModel(category), category.version());
    }

    /**
     * Creates a category.
     *
     * @param request category data
     * @return 201 with {@code Location}
     */
    @PostMapping
    public ResponseEntity<CategoryModel> create(@Valid @RequestBody CategoryRequest request) {
        Category category = save.create(request.name().strip(), request.parentId(), request.color().toUpperCase(),
                request.icon());
        CategoryModel model = assembler.toModel(category);
        return ResponseEntity.created(model.getRequiredLink("self").toUri()).eTag(ETags.of(category.version()))
                .body(model);
    }

    /**
     * Replaces a category.
     *
     * @param id      category id
     * @param request new data
     * @param ifMatch version being edited
     * @return the updated category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryModel> replace(@PathVariable long id, @Valid @RequestBody CategoryRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        Category category = save.update(id, request.name().strip(), request.parentId(), request.color().toUpperCase(),
                request.icon(), ETags.expectedVersion(ifMatch));
        return ETags.ok(assembler.toModel(category), category.version());
    }

    /**
     * Deletes a category nothing points to.
     *
     * @param id category id
     * @return 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable long id) {
        save.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Creates the initial categories in a language, if the user has none yet (I18N-07).
     *
     * @param request language chosen on first use
     * @return the categories created (empty if there were already user categories)
     */
    @PostMapping("/defaults")
    public CollectionModel<CategoryModel> defaults(@Valid @RequestBody InitialSetRequest request) {
        return assembler.toCollectionModel(save.seedInitial(Locale.forLanguageTag(request.language())));
    }

    /**
     * Adds {@code categories} to the API entry point.
     *
     * @return the root link contributor
     */
    @Bean
    static RootLinkContributor categoriesRootLink() {
        return () -> Link.of("/api/categories", "categories");
    }
}

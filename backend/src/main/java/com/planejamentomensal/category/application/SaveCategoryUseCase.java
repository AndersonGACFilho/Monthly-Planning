package com.planejamentomensal.category.application;

import com.planejamentomensal.category.domain.Category;
import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.Versions;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, edits, seeds and deletes categories (RF-06, RF-07, I18N-07).
 *
 * @since 1.0
 */
@Service
public class SaveCategoryUseCase {

    private final CategoryRepository categories;
    private final CategoryQueries queries;

    /**
     * Creates the use case.
     *
     * @param categories category storage
     * @param queries    category queries
     */
    public SaveCategoryUseCase(CategoryRepository categories, CategoryQueries queries) {
        this.categories = categories;
        this.queries = queries;
    }

    /**
     * Creates a category.
     *
     * @param name     name
     * @param parentId parent, or {@code null}
     * @param color    colour
     * @param icon     icon
     * @return the stored category
     * @throws BusinessRuleException with code {@code category-parent-invalid} for a nested or system parent
     */
    @Transactional
    public Category create(String name, Long parentId, String color, String icon) {
        validateParent(parentId, null);
        return categories.saveAndFlush(new Category(name, parentId, color, icon, false));
    }

    /**
     * Edits a category.
     *
     * @param id              category id
     * @param name            name
     * @param parentId        parent, or {@code null}
     * @param color           colour
     * @param icon            icon
     * @param expectedVersion version the client edited, or {@code null}
     * @return the updated category
     */
    @Transactional
    public Category update(long id, String name, Long parentId, String color, String icon, Long expectedVersion) {
        Category category = queries.get(id);
        Versions.check(expectedVersion, category.version());
        validateParent(parentId, id);
        if (parentId != null && categories.countByParentId(id) > 0) {
            throw new BusinessRuleException("category-parent-invalid", parentId);
        }
        category.update(name, parentId, color, icon);
        return categories.saveAndFlush(category);
    }

    /**
     * Deletes a category nothing points to.
     *
     * @param id category id
     * @throws BusinessRuleException with code {@code category-in-use} otherwise
     */
    @Transactional
    public void delete(long id) {
        Category category = queries.get(id);
        if (!queries.isDeletable(id)) {
            throw new BusinessRuleException("category-in-use", category.name());
        }
        categories.delete(category);
    }

    /**
     * Creates the initial categories in a language when the user has none yet (I18N-07).
     *
     * @param locale language chosen on first use
     * @return the categories created, empty if the user already had categories
     */
    @Transactional
    public List<Category> seedInitial(Locale locale) {
        queries.unclassifiedId();
        boolean hasUserCategories = categories.findAll().stream().anyMatch(c -> !c.system());
        if (hasUserCategories) {
            return List.of();
        }
        return InitialCategories.ALL.stream()
                .map(entry -> categories.saveAndFlush(
                        new Category(queries.name(entry.key(), locale), null, entry.color(), entry.icon(), false)))
                .toList();
    }

    private void validateParent(Long parentId, Long ownId) {
        if (parentId == null) {
            return;
        }
        Category parent = categories.findById(parentId)
                .orElseThrow(() -> new BusinessRuleException("category-parent-invalid", parentId));
        if (parent.system() || parent.parentId() != null || parent.id().equals(ownId)) {
            throw new BusinessRuleException("category-parent-invalid", parentId);
        }
    }
}

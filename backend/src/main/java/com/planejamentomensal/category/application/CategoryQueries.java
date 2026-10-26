package com.planejamentomensal.category.application;

import com.planejamentomensal.category.domain.Category;
import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read access to categories, including the "not classified" category every module falls back to (RF-07).
 *
 * @since 1.0
 */
@Service
@Transactional(readOnly = true)
public class CategoryQueries {

    private final CategoryRepository categories;
    private final List<CategoryReferences> references;
    private final MessageSource messages;
    private final Settings settings;

    /**
     * Creates the queries.
     *
     * @param categories category storage
     * @param references reference checks registered by other modules
     * @param messages   message bundles for category names
     * @param settings   user settings, for the language
     */
    public CategoryQueries(CategoryRepository categories, List<CategoryReferences> references, MessageSource messages,
            Settings settings) {
        this.categories = categories;
        this.references = references;
        this.messages = messages;
        this.settings = settings;
    }

    /**
     * Loads a category.
     *
     * @param id id
     * @return the category
     * @throws NotFoundException if it does not exist
     */
    public Category get(long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("category", id));
    }

    /**
     * Returns the view of a category.
     *
     * @param id id
     * @return the view, or empty
     */
    public Optional<CategoryView> find(long id) {
        return categories.findById(id).map(CategoryView::of);
    }

    /**
     * Lists every category by name.
     *
     * @return views of all categories
     */
    public List<CategoryView> all() {
        return categories.findAll().stream().map(CategoryView::of)
                .sorted(Comparator.comparing(CategoryView::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    /**
     * Returns the id of "not classified", creating it in the user's language the first time (RF-07, I18N-07).
     *
     * @return id of the system category
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public long unclassifiedId() {
        return categories.findFirstBySystemTrue()
                .orElseGet(() -> categories.saveAndFlush(new Category(
                        name(InitialCategories.UNCLASSIFIED.key(), language()), null,
                        InitialCategories.UNCLASSIFIED.color(), InitialCategories.UNCLASSIFIED.icon(), true)))
                .id();
    }

    /**
     * Tells whether a category can be deleted.
     *
     * @param id category id
     * @return {@code true} if it is not the system category, has no children and nothing points to it
     */
    public boolean isDeletable(long id) {
        Category category = get(id);
        return !category.system() && categories.countByParentId(id) == 0
                && references.stream().noneMatch(check -> check.isReferenced(id));
    }

    /**
     * Returns the localized name of an initial category.
     *
     * @param key    initial category key
     * @param locale language
     * @return name
     */
    String name(String key, Locale locale) {
        return messages.getMessage("category.initial." + key, null, key, locale);
    }

    private Locale language() {
        String language = settings.get(Setting.LANGUAGE);
        return Locale.forLanguageTag(language.isEmpty() ? "pt-BR" : language);
    }
}

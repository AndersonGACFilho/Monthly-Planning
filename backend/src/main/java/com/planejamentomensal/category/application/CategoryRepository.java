package com.planejamentomensal.category.application;

import com.planejamentomensal.category.domain.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Storage port of categories (SOL-04).
 *
 * @since 1.0
 */
public interface CategoryRepository {

    /**
     * Finds a category.
     *
     * @param id id
     * @return the category, or empty
     */
    Optional<Category> findById(Long id);

    /**
     * Lists categories.
     *
     * @param pageable page and sort
     * @return one page
     */
    Page<Category> findAll(Pageable pageable);

    /**
     * Lists every category.
     *
     * @return all categories
     */
    List<Category> findAll();

    /**
     * Finds the system category.
     *
     * @return "not classified", or empty before it is created
     */
    Optional<Category> findFirstBySystemTrue();

    /**
     * Counts children of a category.
     *
     * @param parentId parent id
     * @return number of subcategories
     */
    long countByParentId(Long parentId);

    /**
     * Stores a category and writes it at once.
     *
     * @param category category
     * @return the stored category
     */
    Category saveAndFlush(Category category);

    /**
     * Removes a category.
     *
     * @param category category
     */
    void delete(Category category);
}

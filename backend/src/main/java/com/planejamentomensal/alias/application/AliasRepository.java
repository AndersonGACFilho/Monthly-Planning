package com.planejamentomensal.alias.application;

import com.planejamentomensal.alias.domain.Alias;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of aliases (SOL-04).
 *
 * @since 1.0
 */
public interface AliasRepository {

    /**
     * Finds an alias.
     *
     * @param id id
     * @return the alias, or empty
     */
    Optional<Alias> findById(Long id);

    /**
     * Finds the alias of a normalized description.
     *
     * @param pattern normalized description
     * @return the alias, or empty
     */
    Optional<Alias> findByPatternsContaining(String pattern);

    /**
     * Lists every alias.
     *
     * @return aliases
     */
    List<Alias> findAll();

    /**
     * Tells whether an alias uses a category.
     *
     * @param categoryId category
     * @return {@code true} if some alias has it as default
     */
    boolean existsByDefaultCategoryId(Long categoryId);

    /**
     * Stores an alias.
     *
     * @param alias alias
     * @return the stored alias
     */
    Alias save(Alias alias);

    /**
     * Removes an alias.
     *
     * @param alias alias
     */
    void delete(Alias alias);
}

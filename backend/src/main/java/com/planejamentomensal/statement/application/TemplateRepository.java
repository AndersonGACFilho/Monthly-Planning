package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.Template;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of {@link Template} (SOL-04).
 *
 * @since 1.0
 */
public interface TemplateRepository {

    /**
     * Finds one by id.
     *
     * @param id id
     * @return the record, or empty
     */
    Optional<Template> findById(Long id);

    /**
     * Lists every template by name.
     *
     * @return templates
     */
    List<Template> findAllByOrderByNameAsc();

    /**
     * Finds the default template.
     *
     * @return the default template, or empty
     */
    Optional<Template> findFirstByDefaultTemplateTrue();

    /**
     * Removes a template.
     *
     * @param template template
     */
    void delete(Template template);

    /**
     * Stores a record and writes it at once.
     *
     * @param value record
     * @return the stored record
     */
    Template saveAndFlush(Template value);
}

package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.TemplateVersion;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of {@link TemplateVersion} (SOL-04).
 *
 * @since 1.0
 */
public interface TemplateVersionRepository {

    /**
     * Finds one by id.
     *
     * @param id id
     * @return the record, or empty
     */
    Optional<TemplateVersion> findById(Long id);

    /**
     * Lists the versions of a template, newest first.
     *
     * @param templateId template
     * @return versions
     */
    List<TemplateVersion> findByTemplateIdOrderByNumberDesc(long templateId);

    /**
     * Finds one version of a template.
     *
     * @param templateId template
     * @param number     version number
     * @return the version, or empty
     */
    Optional<TemplateVersion> findByTemplateIdAndNumber(long templateId, int number);

    /**
     * Stores a record and writes it at once.
     *
     * @param value record
     * @return the stored record
     */
    TemplateVersion saveAndFlush(TemplateVersion value);
}

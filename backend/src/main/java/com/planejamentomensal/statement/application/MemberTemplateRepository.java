package com.planejamentomensal.statement.application;

import com.planejamentomensal.statement.domain.MemberTemplate;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of {@link MemberTemplate} (SOL-04).
 *
 * @since 1.0
 */
public interface MemberTemplateRepository {

    /**
     * Finds one by id.
     *
     * @param id id
     * @return the record, or empty
     */
    Optional<MemberTemplate> findById(Long id);

    /**
     * Lists every member choice.
     *
     * @return choices
     */
    List<MemberTemplate> findAll();

    /**
     * Lists the members that use a template.
     *
     * @param templateId template
     * @return choices
     */
    List<MemberTemplate> findByTemplateId(long templateId);

    /**
     * Removes a choice.
     *
     * @param choice choice
     */
    void delete(MemberTemplate choice);

    /**
     * Stores a record and writes it at once.
     *
     * @param value record
     * @return the stored record
     */
    MemberTemplate saveAndFlush(MemberTemplate value);
}

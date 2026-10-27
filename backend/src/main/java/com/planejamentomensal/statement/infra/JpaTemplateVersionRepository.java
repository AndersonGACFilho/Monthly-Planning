package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.TemplateVersionRepository;
import com.planejamentomensal.statement.domain.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link TemplateVersionRepository}.
 *
 * @since 1.0
 */
public interface JpaTemplateVersionRepository extends JpaRepository<TemplateVersion, Long>, TemplateVersionRepository {
}

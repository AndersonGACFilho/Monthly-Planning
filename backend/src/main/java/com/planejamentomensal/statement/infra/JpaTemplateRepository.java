package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.TemplateRepository;
import com.planejamentomensal.statement.domain.Template;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link TemplateRepository}.
 *
 * @since 1.0
 */
public interface JpaTemplateRepository extends JpaRepository<Template, Long>, TemplateRepository {
}

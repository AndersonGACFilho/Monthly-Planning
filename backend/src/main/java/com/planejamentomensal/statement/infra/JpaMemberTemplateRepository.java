package com.planejamentomensal.statement.infra;

import com.planejamentomensal.statement.application.MemberTemplateRepository;
import com.planejamentomensal.statement.domain.MemberTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link MemberTemplateRepository}.
 *
 * @since 1.0
 */
public interface JpaMemberTemplateRepository extends JpaRepository<MemberTemplate, Long>, MemberTemplateRepository {
}

package com.planejamentomensal.imports.infra;

import com.planejamentomensal.imports.application.ImportLineRepository;
import com.planejamentomensal.imports.domain.ImportLine;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link ImportLineRepository}.
 *
 * @since 1.0
 */
public interface JpaImportLineRepository extends JpaRepository<ImportLine, Long>, ImportLineRepository {
}

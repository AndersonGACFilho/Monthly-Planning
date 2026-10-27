package com.planejamentomensal.imports.infra;

import com.planejamentomensal.imports.application.ImportBatchRepository;
import com.planejamentomensal.imports.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link ImportBatchRepository}.
 *
 * @since 1.0
 */
public interface JpaImportBatchRepository extends JpaRepository<ImportBatch, Long>, ImportBatchRepository {
}

package com.planejamentomensal.entry.infra;

import com.planejamentomensal.entry.application.BatchChangeRepository;
import com.planejamentomensal.entry.domain.BatchChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA implementation of {@link BatchChangeRepository}.
 *
 * @since 1.0
 */
public interface JpaBatchChangeRepository extends JpaRepository<BatchChange, Long>, BatchChangeRepository {

    @Override
    @Modifying
    @Query("delete from BatchChange c where c.batchId = :batchId")
    void deleteByBatchId(long batchId);
}

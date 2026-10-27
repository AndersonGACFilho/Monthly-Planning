package com.planejamentomensal.entry.infra;

import com.planejamentomensal.entry.application.BillEntryRepository;
import com.planejamentomensal.entry.domain.BillEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link BillEntryRepository}.
 *
 * @since 1.0
 */
public interface JpaBillEntryRepository extends JpaRepository<BillEntry, Long>, BillEntryRepository {
}

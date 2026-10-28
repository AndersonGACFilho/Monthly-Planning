package com.planejamentomensal.entry.infra;

import com.planejamentomensal.entry.application.BillEntryRepository;
import com.planejamentomensal.entry.domain.BillEntry;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA implementation of {@link BillEntryRepository}.
 *
 * @since 1.0
 */
public interface JpaBillEntryRepository extends JpaRepository<BillEntry, Long>, BillEntryRepository {

    @Override
    @Query("select distinct e.purchase.id from BillEntry e where e.purchase.id in :purchaseIds and e.importBatchId is not null")
    Set<Long> findImportedPurchaseIds(Collection<Long> purchaseIds);
}

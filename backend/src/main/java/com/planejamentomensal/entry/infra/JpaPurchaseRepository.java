package com.planejamentomensal.entry.infra;

import com.planejamentomensal.entry.application.PurchaseRepository;
import com.planejamentomensal.entry.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link PurchaseRepository}.
 *
 * @since 1.0
 */
public interface JpaPurchaseRepository extends JpaRepository<Purchase, Long>, PurchaseRepository {
}

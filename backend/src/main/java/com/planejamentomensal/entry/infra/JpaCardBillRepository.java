package com.planejamentomensal.entry.infra;

import com.planejamentomensal.entry.application.CardBillRepository;
import com.planejamentomensal.entry.domain.CardBill;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link CardBillRepository}.
 *
 * @since 1.0
 */
public interface JpaCardBillRepository extends JpaRepository<CardBill, Long>, CardBillRepository {
}

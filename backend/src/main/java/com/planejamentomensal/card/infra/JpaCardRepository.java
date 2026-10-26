package com.planejamentomensal.card.infra;

import com.planejamentomensal.card.application.CardRepository;
import com.planejamentomensal.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link CardRepository}.
 *
 * @since 1.0
 */
public interface JpaCardRepository extends JpaRepository<Card, Long>, CardRepository {
}

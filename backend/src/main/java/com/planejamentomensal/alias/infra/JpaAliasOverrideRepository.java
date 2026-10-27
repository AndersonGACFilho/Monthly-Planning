package com.planejamentomensal.alias.infra;

import com.planejamentomensal.alias.application.AliasOverrideRepository;
import com.planejamentomensal.alias.domain.AliasOverride;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA implementation of {@link AliasOverrideRepository}.
 *
 * @since 1.0
 */
public interface JpaAliasOverrideRepository extends JpaRepository<AliasOverride, Long>, AliasOverrideRepository {
}

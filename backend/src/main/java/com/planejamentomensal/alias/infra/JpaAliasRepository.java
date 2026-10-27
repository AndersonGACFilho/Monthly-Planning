package com.planejamentomensal.alias.infra;

import com.planejamentomensal.alias.application.AliasRepository;
import com.planejamentomensal.alias.domain.Alias;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * JPA implementation of {@link AliasRepository}.
 *
 * @since 1.0
 */
public interface JpaAliasRepository extends JpaRepository<Alias, Long>, AliasRepository {

    @Override
    @Query("select a from Alias a where :pattern member of a.patterns")
    Optional<Alias> findByPatternsContaining(String pattern);
}

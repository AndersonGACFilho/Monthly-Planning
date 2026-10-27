package com.planejamentomensal.alias.application;

import com.planejamentomensal.alias.domain.Alias;
import java.util.Set;

/**
 * Read model of an alias for other modules (SOL-06).
 *
 * @param id                id
 * @param displayName       name
 * @param defaultCategoryId default category, or {@code null}
 * @param patterns          normalized descriptions
 * @since 1.0
 */
public record AliasView(long id, String displayName, Long defaultCategoryId, Set<String> patterns) {

    /**
     * Builds the view of an alias.
     *
     * @param alias alias
     * @return its view
     */
    public static AliasView of(Alias alias) {
        return new AliasView(alias.id(), alias.displayName(), alias.defaultCategoryId(), alias.patterns());
    }
}

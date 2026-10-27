package com.planejamentomensal.alias.application;

import com.planejamentomensal.alias.domain.AliasOverride;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of per-entry names (RF-48).
 *
 * @since 1.0
 */
public interface AliasOverrideRepository {

    /**
     * Finds the override of an entry.
     *
     * @param billEntryId entry
     * @return the override, or empty
     */
    Optional<AliasOverride> findById(Long billEntryId);

    /**
     * Lists the overrides of entries.
     *
     * @param billEntryIds entries
     * @return overrides
     */
    List<AliasOverride> findByBillEntryIdIn(Collection<Long> billEntryIds);

    /**
     * Stores an override.
     *
     * @param override override
     * @return the stored override
     */
    AliasOverride save(AliasOverride override);

    /**
     * Removes the override of an entry.
     *
     * @param billEntryId entry
     */
    void deleteById(Long billEntryId);
}

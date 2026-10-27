package com.planejamentomensal.alias.application;

import com.planejamentomensal.alias.domain.Alias;
import com.planejamentomensal.alias.domain.AliasOverride;
import com.planejamentomensal.alias.domain.DisplayDescriptionResolver;
import com.planejamentomensal.shared.domain.DescriptionNormalizer;
import com.planejamentomensal.shared.domain.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, replaces and removes aliases and per-entry names, and resolves displayed descriptions
 * (RF-46 to RF-51, RN-24).
 *
 * @since 1.0
 */
@Service
@Transactional
public class AliasService {

    private final AliasRepository aliases;
    private final AliasOverrideRepository overrides;

    /**
     * Creates the service.
     *
     * @param aliases   alias storage
     * @param overrides per-entry name storage
     */
    public AliasService(AliasRepository aliases, AliasOverrideRepository overrides) {
        this.aliases = aliases;
        this.overrides = overrides;
    }

    /**
     * Names every entry with an original description: creates the alias or replaces its name, always
     * pointing to the original description, never to the previous alias (RF-46, RF-47, RF-48).
     *
     * @param originalDescription original description of the entry being renamed
     * @param displayName         new name
     * @param defaultCategoryId   category for future imports, or {@code null} to keep the current one (RF-49)
     * @return the alias
     */
    public AliasView renameAll(String originalDescription, String displayName, Long defaultCategoryId) {
        String pattern = DescriptionNormalizer.normalize(originalDescription);
        Alias alias = aliases.findByPatternsContaining(pattern).map(existing -> {
            existing.rename(displayName);
            if (defaultCategoryId != null) {
                existing.defaultCategory(defaultCategoryId);
            }
            return existing;
        }).orElseGet(() -> new Alias(displayName, pattern, defaultCategoryId));
        return AliasView.of(aliases.save(alias));
    }

    /**
     * Names one entry only, keeping the alias of its description (RF-48).
     *
     * @param billEntryId entry
     * @param displayName name for that entry
     */
    public void renameOne(long billEntryId, String displayName) {
        AliasOverride override = overrides.findById(billEntryId).map(existing -> {
            existing.rename(displayName);
            return existing;
        }).orElseGet(() -> new AliasOverride(billEntryId, displayName));
        overrides.save(override);
    }

    /**
     * Shows the card's description again: removes the entry's own name, or else the alias of its
     * description (only that description when the alias groups several, RN-24) (RF-50).
     *
     * @param billEntryId         entry
     * @param originalDescription its original description
     */
    public void restoreOriginal(long billEntryId, String originalDescription) {
        if (overrides.findById(billEntryId).isPresent()) {
            overrides.deleteById(billEntryId);
            return;
        }
        String pattern = DescriptionNormalizer.normalize(originalDescription);
        aliases.findByPatternsContaining(pattern).ifPresent(alias -> {
            if (alias.removePattern(pattern)) {
                aliases.delete(alias);
            } else {
                aliases.save(alias);
            }
        });
    }

    /**
     * Adds a description of the same store to an alias (RN-24).
     *
     * @param aliasId             alias
     * @param originalDescription description to group
     * @return the alias
     */
    public AliasView addDescription(long aliasId, String originalDescription) {
        Alias alias = aliases.findById(aliasId).orElseThrow(() -> new NotFoundException("alias", aliasId));
        String pattern = DescriptionNormalizer.normalize(originalDescription);
        aliases.findByPatternsContaining(pattern).filter(other -> !other.id().equals(alias.id())).ifPresent(other -> {
            if (other.removePattern(pattern)) {
                aliases.delete(other);
            } else {
                aliases.save(other);
            }
        });
        alias.addPattern(pattern);
        return AliasView.of(aliases.save(alias));
    }

    /**
     * Returns the alias of a description.
     *
     * @param originalDescription original description
     * @return the alias, or empty
     */
    @Transactional(readOnly = true)
    public Optional<AliasView> aliasOf(String originalDescription) {
        return aliases.findByPatternsContaining(DescriptionNormalizer.normalize(originalDescription)).map(AliasView::of);
    }

    /**
     * Suggests an alias of the same store for a description that has none (RN-24).
     *
     * @param originalDescription original description
     * @return an alias whose pattern is the same store by prefix, or empty
     */
    @Transactional(readOnly = true)
    public Optional<AliasView> sameStoreAlias(String originalDescription) {
        String pattern = DescriptionNormalizer.normalize(originalDescription);
        if (aliases.findByPatternsContaining(pattern).isPresent()) {
            return Optional.empty();
        }
        return aliases.findAll().stream()
                .filter(a -> a.patterns().stream().anyMatch(p -> DescriptionNormalizer.sameStore(p, pattern)))
                .findFirst().map(AliasView::of);
    }

    /**
     * Resolves displayed descriptions for many entries at once (RF-46 to RF-50).
     *
     * @param entries entry id to original description
     * @return entry id to displayed description, with alias and override flags
     */
    @Transactional(readOnly = true)
    public Map<Long, Display> display(Map<Long, String> entries) {
        Map<Long, String> byEntry = overrides.findByBillEntryIdIn(entries.keySet()).stream()
                .collect(Collectors.toMap(AliasOverride::billEntryId, AliasOverride::displayName));
        Map<String, Alias> byPattern = aliases.findAll().stream()
                .flatMap(a -> a.patterns().stream().map(p -> Map.entry(p, a)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        return entries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> {
            Alias alias = byPattern.get(DescriptionNormalizer.normalize(e.getValue()));
            String override = byEntry.get(e.getKey());
            return new Display(DisplayDescriptionResolver.resolve(override, alias == null ? null : alias.displayName(),
                    e.getValue()), alias == null ? null : alias.id(), override != null);
        }));
    }

    /**
     * Displayed description of one entry.
     *
     * @param text        description to show
     * @param aliasId     alias of its description, or {@code null}
     * @param overridden  {@code true} if the entry has its own name
     */
    public record Display(String text, Long aliasId, boolean overridden) {

        /**
         * Tells whether a name other than the original is shown (link {@code restore-original}).
         *
         * @return {@code true} with an alias or an override
         */
        public boolean renamed() {
            return aliasId != null || overridden;
        }
    }

    /**
     * Lists every alias.
     *
     * @return aliases by id
     */
    @Transactional(readOnly = true)
    public Map<Long, AliasView> all() {
        return aliases.findAll().stream().map(AliasView::of)
                .collect(Collectors.toMap(AliasView::id, Function.identity()));
    }

    /**
     * Returns the overrides of entries.
     *
     * @param entryIds entries
     * @return names by entry
     */
    @Transactional(readOnly = true)
    public Map<Long, String> overridesOf(Collection<Long> entryIds) {
        return overrides.findByBillEntryIdIn(entryIds).stream()
                .collect(Collectors.toMap(AliasOverride::billEntryId, AliasOverride::displayName));
    }

    /**
     * Lists aliases whose names or patterns contain a text (RF-51).
     *
     * @param text search text
     * @return matching aliases
     */
    @Transactional(readOnly = true)
    public List<AliasView> search(String text) {
        String needle = text.toLowerCase(java.util.Locale.ROOT);
        String normalized = DescriptionNormalizer.normalize(text);
        return aliases.findAll().stream()
                .filter(a -> a.displayName().toLowerCase(java.util.Locale.ROOT).contains(needle)
                        || a.patterns().stream().anyMatch(p -> !normalized.isEmpty() && p.contains(normalized)))
                .map(AliasView::of).toList();
    }
}

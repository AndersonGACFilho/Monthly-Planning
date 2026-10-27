package com.planejamentomensal.imports.application;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.alias.application.AliasView;
import com.planejamentomensal.entry.application.EntryQueries;
import com.planejamentomensal.imports.domain.ImportLine;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Fills preview lines with the alias and category already chosen for their description: the alias name, its
 * default category (RF-49, RF-70), otherwise the category last used for the same description (RF-08).
 *
 * @since 1.0
 */
@Component
public class SuggestionEnricher implements PreviewEnricher {

    private final AliasService aliases;
    private final EntryQueries entries;

    /**
     * Creates the enricher.
     *
     * @param aliases alias service
     * @param entries entry queries, for the last category used
     */
    public SuggestionEnricher(AliasService aliases, EntryQueries entries) {
        this.aliases = aliases;
        this.entries = entries;
    }

    @Override
    public void enrich(List<ImportLine> lines) {
        for (ImportLine line : lines) {
            if (line.futureCheck()) {
                continue;
            }
            Optional<AliasView> alias = aliases.aliasOf(line.description());
            Long category = alias.map(AliasView::defaultCategoryId)
                    .or(() -> entries.lastCategoryOf(line.description()))
                    .orElse(null);
            line.edit(null, category, alias.map(AliasView::displayName).orElse(null));
        }
    }
}

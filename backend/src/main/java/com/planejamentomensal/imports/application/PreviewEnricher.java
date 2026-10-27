package com.planejamentomensal.imports.application;

import com.planejamentomensal.imports.domain.ImportLine;
import java.util.List;

/**
 * Adds suggestions to preview lines, such as the alias and category of the description (RF-70). Modules
 * register implementations (SOL-02).
 *
 * @since 1.0
 */
public interface PreviewEnricher {

    /**
     * Adds suggestions to the lines.
     *
     * @param lines preview lines, not yet stored
     */
    void enrich(List<ImportLine> lines);
}

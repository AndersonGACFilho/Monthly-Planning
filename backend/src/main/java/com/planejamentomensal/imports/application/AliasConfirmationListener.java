package com.planejamentomensal.imports.application;

import com.planejamentomensal.alias.application.AliasService;
import com.planejamentomensal.entry.application.ReviewQueue;
import com.planejamentomensal.imports.domain.ImportBatch;
import com.planejamentomensal.imports.domain.ImportLine;
import com.planejamentomensal.shared.application.Setting;
import com.planejamentomensal.shared.application.Settings;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * After confirmation: stores the aliases chosen in the preview for their descriptions (RF-70) and, when the
 * setting is on, validates items whose description has an alias with a default category (RF-79d).
 *
 * @since 1.0
 */
@Component
public class AliasConfirmationListener implements ConfirmationListener {

    private final AliasService aliases;
    private final ReviewQueue queue;
    private final Settings settings;

    /**
     * Creates the listener.
     *
     * @param aliases  alias service
     * @param queue    review queue
     * @param settings user settings
     */
    public AliasConfirmationListener(AliasService aliases, ReviewQueue queue, Settings settings) {
        this.aliases = aliases;
        this.queue = queue;
        this.settings = settings;
    }

    @Override
    public void confirmed(ImportBatch batch, List<ImportLine> lines) {
        for (ImportLine line : lines) {
            if (line.createdEntryId() == null || line.aliasName() == null) {
                continue;
            }
            boolean sameAsAlias = aliases.aliasOf(line.description()).map(a -> a.displayName().equals(line.aliasName())).orElse(false);
            if (!sameAsAlias) {
                aliases.renameAll(line.description(), line.aliasName(), line.categoryId());
            }
        }
        if (!settings.getBoolean(Setting.AUTO_VALIDATE)) {
            return;
        }
        List<Long> ready = queue.pending(null, null, batch.id()).stream()
                .filter(item -> aliases.aliasOf(item.originalDescription()).map(a -> a.defaultCategoryId() != null).orElse(false))
                .filter(item -> item.categoryId() != null)
                .map(item -> item.purchaseId())
                .toList();
        queue.validateWhereComplete(ready);
    }
}

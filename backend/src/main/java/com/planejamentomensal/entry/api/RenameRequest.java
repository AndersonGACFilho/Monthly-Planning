package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.EntryCommands;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Rename of an entry's description (RF-46 to RF-49).
 *
 * @param displayName       new name
 * @param scope             {@code ALL} (default) or {@code ONLY_THIS}
 * @param defaultCategoryId category for future imports, optional
 * @since 1.0
 */
public record RenameRequest(@NotBlank @Size(max = 80) String displayName, EntryCommands.RenameScope scope,
        Long defaultCategoryId) {
}

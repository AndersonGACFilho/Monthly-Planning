package com.planejamentomensal.entry.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Same changes for several selected items, optionally validating them (RF-77).
 *
 * @param purchaseIds selected purchases
 * @param changes     changes to apply
 * @param validate    {@code true} to validate the selected items
 * @since 1.0
 */
public record BatchValidationRequest(@NotEmpty List<Long> purchaseIds, @NotNull @Valid ReviewEditRequest changes,
        boolean validate) {
}

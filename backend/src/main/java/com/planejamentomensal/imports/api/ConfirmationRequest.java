package com.planejamentomensal.imports.api;

/**
 * Confirmation of a preview (RF-72).
 *
 * @param acknowledgeTotals {@code true} when the user reviewed a totals difference (RF-64h)
 * @since 1.0
 */
public record ConfirmationRequest(boolean acknowledgeTotals) {
}

package com.planejamentomensal.category.domain;

/**
 * Editable data of a category (RF-06, IMP-65).
 *
 * @param name         name
 * @param parentId     parent category, or {@code null}
 * @param color        colour as {@code #RRGGBB}
 * @param icon         icon name from the interface icon set
 * @param subscription {@code true} when single card purchases in it are subscriptions
 * @since 1.0
 */
public record CategoryDetails(String name, Long parentId, String color, String icon, boolean subscription) {
}

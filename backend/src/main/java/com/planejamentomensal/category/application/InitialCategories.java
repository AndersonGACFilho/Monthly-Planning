package com.planejamentomensal.category.application;

import java.util.List;

/**
 * Categories created on first use, with colour and icon; names come from the message bundle of the chosen
 * language (I18N-07).
 *
 * @since 1.0
 */
public final class InitialCategories {

    /**
     * One initial category.
     *
     * @param key   message key suffix ({@code category.initial.<key>})
     * @param color colour
     * @param icon  icon name
     */
    public record Entry(String key, String color, String icon) {
    }

    /** Key of the system category "not classified" (RF-07). */
    public static final Entry UNCLASSIFIED = new Entry("unclassified", "#9CA3AF", "circle-help");

    /** Initial user categories (RF-06 examples plus the spreadsheet model's list). */
    public static final List<Entry> ALL = List.of(
            new Entry("groceries", "#16A34A", "shopping-cart"),
            new Entry("food", "#F97316", "utensils"),
            new Entry("transport", "#0EA5E9", "car"),
            new Entry("health", "#EF4444", "heart-pulse"),
            new Entry("leisure", "#A855F7", "party-popper"),
            new Entry("subscriptions", "#6366F1", "repeat"),
            new Entry("home", "#84CC16", "house"),
            new Entry("clothing", "#EC4899", "shirt"),
            new Entry("education", "#14B8A6", "graduation-cap"),
            new Entry("travel", "#F59E0B", "plane"),
            new Entry("pets", "#A16207", "paw-print"),
            new Entry("other", "#64748B", "circle-ellipsis"));

    private InitialCategories() {
    }
}

package com.planejamentomensal.category.application;

import com.planejamentomensal.category.domain.CategoryDetails;
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
     * @param key          message key suffix ({@code category.initial.<key>})
     * @param color        colour
     * @param icon         icon name
     * @param subscription marked as subscription (IMP-65)
     */
    public record Entry(String key, String color, String icon, boolean subscription) {

        /**
         * Returns the data of the category with its name in the chosen language.
         *
         * @param name translated name
         * @return category data without parent
         */
        public CategoryDetails details(String name) {
            return new CategoryDetails(name, null, color, icon, subscription);
        }
    }

    /** Key of the system category "not classified" (RF-07). */
    public static final Entry UNCLASSIFIED = new Entry("unclassified", "#9CA3AF", "circle-help", false);

    /** Initial user categories (RF-06 examples plus the spreadsheet model's list). */
    public static final List<Entry> ALL = List.of(
            new Entry("groceries", "#16A34A", "shopping-cart", false),
            new Entry("food", "#F97316", "utensils", false),
            new Entry("transport", "#0EA5E9", "car", false),
            new Entry("health", "#EF4444", "heart-pulse", false),
            new Entry("leisure", "#A855F7", "party-popper", false),
            new Entry("subscriptions", "#6366F1", "repeat", true),
            new Entry("home", "#84CC16", "house", false),
            new Entry("clothing", "#EC4899", "shirt", false),
            new Entry("education", "#14B8A6", "graduation-cap", false),
            new Entry("travel", "#F59E0B", "plane", false),
            new Entry("pets", "#A16207", "paw-print", false),
            new Entry("other", "#64748B", "circle-ellipsis", false));

    private InitialCategories() {
    }
}

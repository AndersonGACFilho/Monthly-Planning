package com.planejamentomensal.entry.application;

import java.util.List;

/**
 * Changes applied to one or more purchases in the review queue (RF-76, RF-77). Fields left {@code null}
 * keep their current value.
 *
 * @param splits           member shares, or {@code null}
 * @param categoryId       category, or {@code null}
 * @param clearCategory    {@code true} to remove the category (third-party only purchases, RF-76)
 * @param note             note, or {@code null}
 * @param subscription     {@code true} to accept the subscription suggestion (RN-23)
 * @since 1.0
 */
public record ReviewEdit(List<ReviewItem.Share> splits, Long categoryId, boolean clearCategory, String note,
        boolean subscription) {

    /**
     * Edit that changes nothing, for validating as is.
     *
     * @return empty edit
     */
    public static ReviewEdit none() {
        return new ReviewEdit(null, null, false, null, false);
    }
}

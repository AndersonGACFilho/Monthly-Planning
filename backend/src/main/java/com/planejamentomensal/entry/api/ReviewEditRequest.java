package com.planejamentomensal.entry.api;

import com.planejamentomensal.entry.application.ReviewEdit;
import com.planejamentomensal.entry.application.ReviewItem;
import com.planejamentomensal.shared.domain.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * Changes for one or more review items (RF-76, RF-77); {@code null} fields keep their value.
 *
 * @param splits        member shares
 * @param categoryId    category
 * @param clearCategory remove the category (third-party only purchases)
 * @param note          note; blank removes it
 * @param subscription  accept the subscription suggestion (RN-23)
 * @since 1.0
 */
public record ReviewEditRequest(@Valid List<ShareRequest> splits, Long categoryId, boolean clearCategory,
        @Size(max = 500) String note, boolean subscription) {

    /**
     * Share of one member, by percentage or amount.
     *
     * @param memberId   member
     * @param percentage percentage from 0 to 100
     * @param amount     fixed amount per installment
     */
    public record ShareRequest(@NotNull Long memberId, @DecimalMin("0") @DecimalMax("100") BigDecimal percentage,
            @DecimalMin("0") BigDecimal amount) {
    }

    /**
     * Converts to the application edit.
     *
     * @return the edit
     */
    public ReviewEdit toEdit() {
        return new ReviewEdit(splits == null ? null : splits.stream()
                .map(s -> new ReviewItem.Share(s.memberId(), s.percentage(), s.amount() == null ? null : Money.of(s.amount())))
                .toList(), categoryId, clearCategory, note, subscription);
    }
}

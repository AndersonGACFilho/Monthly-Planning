package com.planejamentomensal.entry.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Inline edit of an entry's purchase (RF-43); {@code null} fields keep their value.
 *
 * @param categoryId    category
 * @param clearCategory remove the category
 * @param splits        member shares
 * @param note          note; blank removes it
 * @since 1.0
 */
public record EntryEditRequest(Long categoryId, boolean clearCategory, @Valid List<ReviewEditRequest.ShareRequest> splits,
        @Size(max = 500) String note) {
}

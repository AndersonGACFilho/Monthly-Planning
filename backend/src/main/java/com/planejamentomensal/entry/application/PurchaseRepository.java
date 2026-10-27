package com.planejamentomensal.entry.application;

import com.planejamentomensal.entry.domain.Purchase;
import com.planejamentomensal.entry.domain.ReviewStatus;
import java.util.List;
import java.util.Optional;

/**
 * Storage port of purchases (SOL-04).
 *
 * @since 1.0
 */
public interface PurchaseRepository {

    /**
     * Finds a purchase.
     *
     * @param id id
     * @return the purchase, or empty
     */
    Optional<Purchase> findById(Long id);

    /**
     * Lists the purchases of a card.
     *
     * @param cardId card
     * @return purchases
     */
    List<Purchase> findByCardId(long cardId);

    /**
     * Lists purchases by review state (RF-74).
     *
     * @param reviewStatus state
     * @return purchases
     */
    List<Purchase> findByReviewStatus(ReviewStatus reviewStatus);

    /**
     * Lists purchases with a normalized description.
     *
     * @param normalizedDescription description (RN-15)
     * @return purchases
     */
    List<Purchase> findByNormalizedDescription(String normalizedDescription);

    /**
     * Tells whether a member has a share in some purchase (RF-01).
     *
     * @param memberId member
     * @return {@code true} if referenced
     */
    boolean existsBySplitsMemberId(long memberId);

    /**
     * Tells whether a category is used by some purchase.
     *
     * @param categoryId category
     * @return {@code true} if referenced
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Tells whether a card has purchases.
     *
     * @param cardId card
     * @return {@code true} if referenced
     */
    boolean existsByCardId(long cardId);

    /**
     * Stores a purchase.
     *
     * @param purchase purchase
     * @return the stored purchase
     */
    Purchase save(Purchase purchase);

    /**
     * Removes a purchase.
     *
     * @param purchase purchase
     */
    void delete(Purchase purchase);
}

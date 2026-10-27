package com.planejamentomensal.entry.application;

import com.planejamentomensal.category.application.CategoryReferences;
import org.springframework.stereotype.Component;

/**
 * A category used by purchases cannot be deleted (RF-07).
 *
 * @since 1.0
 */
@Component
public class PurchaseCategoryReferences implements CategoryReferences {

    private final PurchaseRepository purchases;
    private final CardBillRepository bills;

    /**
     * Creates the check.
     *
     * @param purchases purchase storage
     * @param bills     bill storage
     */
    public PurchaseCategoryReferences(PurchaseRepository purchases, CardBillRepository bills) {
        this.purchases = purchases;
        this.bills = bills;
    }

    /**
     * Tells whether purchases use the category.
     *
     * @param categoryId category id
     * @return {@code true} if referenced
     */
    @Override
    public boolean isReferenced(long categoryId) {
        return purchases.existsByCategoryId(categoryId);
    }
}

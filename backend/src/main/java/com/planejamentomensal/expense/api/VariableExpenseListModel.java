package com.planejamentomensal.expense.api;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.hateoas.RepresentationModel;

/**
 * Filtered variable expenses with the filter total (RF-35).
 *
 * @since 1.0
 */
public class VariableExpenseListModel extends RepresentationModel<VariableExpenseListModel> {

    /** Expenses. */
    public final List<VariableExpenseModel> items;
    /** Sum of the listed expenses. */
    public final BigDecimal total;

    /**
     * Creates the list.
     *
     * @param items expenses
     * @param total sum
     */
    public VariableExpenseListModel(List<VariableExpenseModel> items, BigDecimal total) {
        this.items = items;
        this.total = total;
    }
}

package com.planejamentomensal.statement.application.workbook;

import com.planejamentomensal.shared.domain.BillingPeriod;
import com.planejamentomensal.shared.domain.Money;
import java.util.List;

/**
 * Everything the monthly spreadsheet shows, gathered from the other modules before any cell is written
 * (XLS-01 to XLS-07).
 *
 * @param period       billing period
 * @param members      members, MAIN first
 * @param cardRows     rows of the cards sheet, grouped by card in the configured order, "Bills" group last
 * @param incomes      incomes of MAIN members
 * @param expenses     fixed (not card) and variable expenses of MAIN members
 * @param carries      what third parties carried in from the previous period (positive) and out to the next one
 *                     (negative), beside their reimbursements (RF-28)
 * @param categories   category names for the drop-down list
 * @param memberSheets one sheet per member marked in the settings (XLS-06)
 * @since 1.0
 */
public record WorkbookData(BillingPeriod period, List<Member> members, List<CardRow> cardRows, List<Line> incomes,
        List<Line> expenses, List<Line> carries, List<String> categories, List<MemberSheet> memberSheets) {

    /**
     * A member as the spreadsheet sees it (XLS-04).
     *
     * @param id   member
     * @param code code, such as {@code AB}
     * @param name name
     * @param main {@code true} for a MAIN member, {@code false} for a third party who reimburses
     */
    public record Member(long id, String code, String name, boolean main) {
    }

    /**
     * One row of the cards sheet (XLS-02, XLS-03, XLS-05, XLS-07).
     *
     * @param group       card nickname, or the "Bills" group
     * @param dueDay      due day of the card, or the day of the bill
     * @param memberCode  member code
     * @param installment {@code -}, {@code *} or {@code NN/NN}
     * @param description displayed description
     * @param amount      amount (member share)
     * @param category    category for MAIN members, empty for third parties
     * @param pending     still pending review
     */
    public record CardRow(String group, int dueDay, String memberCode, String installment, String description,
            Money amount, String category, boolean pending) {
    }

    /**
     * One income or expense line.
     *
     * @param item   description
     * @param amount amount
     */
    public record Line(String item, Money amount) {
    }

    /**
     * Sheet of one member with blocks of neighbouring months (XLS-06).
     *
     * @param name   sheet name
     * @param blocks one block per month
     */
    public record MemberSheet(String name, List<MonthBlock> blocks) {
    }

    /**
     * Rows of one member in one month.
     *
     * @param period     billing period
     * @param rows       rows, the previous balance included (RF-28)
     * @param carriedOut amount carried into the next month, shown below the block and out of its sum, or {@code null}
     */
    public record MonthBlock(BillingPeriod period, List<CardRow> rows, Line carriedOut) {
    }
}

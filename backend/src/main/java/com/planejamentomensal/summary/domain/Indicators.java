package com.planejamentomensal.summary.domain;

import com.planejamentomensal.shared.domain.Money;
import java.math.BigDecimal;

/**
 * Top indicators of the overview (RF-10, RF-16).
 *
 * @param income       total income
 * @param expense      total expense
 * @param balance      income − expense
 * @param percentSpent expense ÷ income × 100, or {@code null} without income
 * @param alert        alert level by the configured thresholds
 * @since 1.0
 */
public record Indicators(Money income, Money expense, Money balance, BigDecimal percentSpent, AlertLevel alert) {

    /**
     * Alert level of the percentage spent (RF-16).
     */
    public enum AlertLevel {
        /** Below the warning threshold. */
        NONE,
        /** At or above the warning threshold. */
        WARNING,
        /** At or above the danger threshold, or expenses without income. */
        DANGER
    }

    /**
     * Calculates the indicators.
     *
     * @param income        total income
     * @param expense       total expense
     * @param warningPercent warning threshold, e.g. 80
     * @param dangerPercent  danger threshold, e.g. 100
     * @return indicators
     */
    public static Indicators of(Money income, Money expense, int warningPercent, int dangerPercent) {
        BigDecimal percent = Percentages.of(expense, income);
        AlertLevel alert;
        if (percent == null) {
            alert = expense.amount().signum() > 0 ? AlertLevel.DANGER : AlertLevel.NONE;
        } else if (percent.compareTo(BigDecimal.valueOf(dangerPercent)) >= 0) {
            alert = AlertLevel.DANGER;
        } else if (percent.compareTo(BigDecimal.valueOf(warningPercent)) >= 0) {
            alert = AlertLevel.WARNING;
        } else {
            alert = AlertLevel.NONE;
        }
        return new Indicators(income, expense, income.minus(expense), percent, alert);
    }
}

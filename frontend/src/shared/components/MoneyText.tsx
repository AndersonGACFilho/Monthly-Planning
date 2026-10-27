import { useTranslation } from 'react-i18next';
import { formatMoney } from '../format';

/** Props of {@link MoneyText}. */
export interface MoneyTextProps {
  /** Amount as sent by the API. */
  value: number | string | null | undefined;
  /** Extra classes. */
  className?: string;
  /**
   * How a negative amount is read: `credit` (refunds and credits, green, default) or `balance` (money missing,
   * red).
   */
  kind?: 'credit' | 'balance';
}

/**
 * Shows an amount in reais formatted for the interface language; negative amounts are highlighted, green for
 * refunds and credits, red for balances below zero (I18N-05, RNF-02).
 *
 * @param props - {@link MoneyTextProps}
 * @returns the formatted amount
 */
export function MoneyText({ value, className = '', kind = 'credit' }: MoneyTextProps) {
  const { i18n } = useTranslation();
  const negative = Number(value ?? 0) < 0;
  const tone = !negative ? '' : kind === 'balance' ? 'text-red-600 dark:text-red-400' : 'text-emerald-600 dark:text-emerald-400';
  return (
    <span className={`tabular-nums ${tone} ${className}`}>
      {formatMoney(value, i18n.language)}
    </span>
  );
}

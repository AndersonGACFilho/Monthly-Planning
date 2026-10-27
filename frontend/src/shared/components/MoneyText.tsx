import { useTranslation } from 'react-i18next';
import { formatMoney } from '../format';

/** Props of {@link MoneyText}. */
export interface MoneyTextProps {
  /** Amount as sent by the API. */
  value: number | string | null | undefined;
  /** Extra classes. */
  className?: string;
}

/**
 * Shows an amount in reais formatted for the interface language; negative amounts (refunds and credits)
 * are highlighted (I18N-05, RNF-02).
 *
 * @param props - {@link MoneyTextProps}
 * @returns the formatted amount
 */
export function MoneyText({ value, className = '' }: MoneyTextProps) {
  const { i18n } = useTranslation();
  const negative = Number(value ?? 0) < 0;
  return (
    <span className={`tabular-nums ${negative ? 'text-emerald-600 dark:text-emerald-400' : ''} ${className}`}>
      {formatMoney(value, i18n.language)}
    </span>
  );
}

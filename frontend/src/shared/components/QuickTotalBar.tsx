import { useTranslation } from 'react-i18next';
import { fromCents, toCents } from '../format';
import { MoneyText } from './MoneyText';

/** Props of {@link QuickTotalBar}. */
export interface QuickTotalBarProps {
  /** Amounts of the rows left after filters. */
  filtered: (number | string | undefined)[];
  /** Amounts of the selected rows. */
  selected: (number | string | undefined)[];
}

/**
 * Bar fixed at the bottom of the window with the sum and count of the filtered rows and of the selected
 * rows (RF-42, UI-03). Sums are done in cents over amounts already received from the API (SPEC section 13).
 *
 * @param props - {@link QuickTotalBarProps}
 * @returns the bar
 */
export function QuickTotalBar({ filtered, selected }: QuickTotalBarProps) {
  const { t } = useTranslation();
  const sum = (values: (number | string | undefined)[]) => fromCents(values.reduce<number>((total, v) => total + toCents(v), 0));
  return (
    <div
      role="status"
      aria-live="polite"
      className="sticky bottom-0 z-10 flex flex-wrap justify-end gap-6 border-t border-(--color-border) bg-(--color-surface-muted) px-4 py-2 text-sm"
    >
      <span>
        {t('quickTotal.filtered', { count: filtered.length })}: <strong><MoneyText value={sum(filtered)} /></strong>
      </span>
      <span>
        {t('quickTotal.selected', { count: selected.length })}: <strong><MoneyText value={sum(selected)} /></strong>
      </span>
    </div>
  );
}

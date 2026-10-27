import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatPeriod } from '../format';
import { useBillingPeriodStore } from '../state/billingPeriod';

/**
 * Global billing period selector fixed at the top of the window (UI-02). Alt+← and Alt+→ change the
 * month from anywhere (UI-01).
 *
 * @returns the selector
 */
export function BillingPeriodSelector() {
  const { t, i18n } = useTranslation();
  const { period, setPeriod, shift } = useBillingPeriodStore();

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (!event.altKey) return;
      if (event.key === 'ArrowLeft') {
        event.preventDefault();
        shift(-1);
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        shift(1);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [shift]);

  return (
    <div className="flex items-center gap-1" role="group" aria-label={t('period.label')}>
      <button type="button" className="icon-button" onClick={() => shift(-1)} aria-label={t('period.previous')}>
        <ChevronLeft size={18} aria-hidden />
      </button>
      <label className="relative">
        <span className="inline-block min-w-40 px-2 text-center font-medium first-letter:uppercase">{formatPeriod(period, i18n.language)}</span>
        <input
          type="month"
          className="absolute inset-0 cursor-pointer opacity-0"
          value={period}
          aria-label={t('period.label')}
          onChange={(event) => event.target.value && setPeriod(event.target.value)}
        />
      </label>
      <button type="button" className="icon-button" onClick={() => shift(1)} aria-label={t('period.next')}>
        <ChevronRight size={18} aria-hidden />
      </button>
    </div>
  );
}

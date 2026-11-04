import { CalendarCheck, ChevronLeft, ChevronRight } from 'lucide-react';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatPeriod, periodOf } from '../format';
import { useBillingPeriodStore } from '../state/billingPeriod';

/**
 * Says how far a period is from the current one, such as "last month" or "in 2 months".
 *
 * @param today    - current period, `yyyy-MM`
 * @param period   - selected period, `yyyy-MM`
 * @param language - interface language
 * @returns the distance in words
 */
export function relativeMonths(today: string, period: string, language: string): string {
  const [ty, tm] = today.split('-').map(Number);
  const [py, pm] = period.split('-').map(Number);
  const months = (py! - ty!) * 12 + (pm! - tm!);
  return new Intl.RelativeTimeFormat(language, { numeric: 'auto' }).format(months, 'month');
}

/**
 * Global billing period selector fixed at the top of the window (UI-02). Alt+← and Alt+→ change the
 * month from anywhere (UI-01). The current month stands out with the accent colour and a "current month" badge;
 * any other month shows a button back to the current one (IMP-55).
 *
 * @returns the selector
 */
export function BillingPeriodSelector() {
  const { t, i18n } = useTranslation();
  const { period, setPeriod, shift } = useBillingPeriodStore();
  const today = periodOf();
  const current = period === today;

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
      <label
        className={`relative flex flex-col items-center rounded-md px-3 py-0.5 ${
          current ? 'bg-(--color-accent) text-(--color-surface)' : 'border border-dashed border-(--color-border)'
        }`}
      >
        <span className="inline-block min-w-40 text-center text-base font-semibold first-letter:uppercase">
          {formatPeriod(period, i18n.language)}
        </span>
        <span
          id="period-relation"
          className={`text-[10px] leading-3 tracking-wide uppercase ${current ? 'opacity-90' : 'text-(--color-text-muted)'}`}
        >
          {current ? t('period.current') : relativeMonths(today, period, i18n.language)}
        </span>
        <input
          type="month"
          className="absolute inset-0 cursor-pointer opacity-0"
          value={period}
          aria-label={t('period.label')}
          aria-describedby="period-relation"
          onChange={(event) => event.target.value && setPeriod(event.target.value)}
        />
      </label>
      <button type="button" className="icon-button" onClick={() => shift(1)} aria-label={t('period.next')}>
        <ChevronRight size={18} aria-hidden />
      </button>
      {!current && (
        <button type="button" className="button ml-1 whitespace-nowrap" onClick={() => setPeriod(today)} title={formatPeriod(today, i18n.language)}>
          <CalendarCheck size={14} aria-hidden />
          <span className="hidden wide:inline">{t('period.goToCurrent')}</span>
          <span className="sr-only wide:hidden">{t('period.goToCurrent')}</span>
        </button>
      )}
    </div>
  );
}

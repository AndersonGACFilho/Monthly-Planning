import { useTranslation } from 'react-i18next';
import { Bar, BarChart, CartesianGrid, Cell, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { useHistory } from '../../api/generated/endpoints/history-controller/history-controller';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatMoney, formatPercent, formatPeriod, shiftPeriod } from '../../shared/format';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';

/** Months shown in the chart, the selected one last. */
export const LEFTOVER_MONTHS = 6;

const POSITIVE = '#2563eb';
const NEGATIVE = '#dc2626';
const FADED = 0.4;
const axis = { tick: { fill: 'currentColor', fontSize: 12 }, stroke: 'currentColor' };

/** Props of {@link LeftoverChart}. */
export interface LeftoverChartProps {
  /** Selected billing period. */
  period: string;
  /** Member filter of the overview, or `null` for everyone. */
  member: number | null;
  /** Income of the selected period, from the overview. */
  income: number | undefined;
  /** Expense of the selected period, from the overview. */
  expense: number | undefined;
  /** What was left in the selected period (income − expense), from the overview. */
  balance: number | undefined;
}

/**
 * Share of the income that was spent and what was left, as a bar.
 *
 * @param income  - income of the month
 * @param expense - expense of the month
 * @returns spent and left in percent of the income, both within 0–100, or `null` without income
 */
export function leftoverShares(income: number, expense: number): { spent: number; left: number } | null {
  if (income <= 0) return null;
  const spent = Math.min(100, Math.max(0, (expense / income) * 100));
  return { spent, left: 100 - spent };
}

/**
 * What was left of the income in the selected month, as a headline, a spent-versus-left bar and the leftover of the
 * last months with the selected one highlighted; a month in the chart becomes the selected period (RF-10).
 *
 * @param props - {@link LeftoverChartProps}
 * @returns the section
 */
export function LeftoverChart({ period, member, income = 0, expense = 0, balance = 0 }: LeftoverChartProps) {
  const { t, i18n } = useTranslation();
  const setPeriod = useBillingPeriodStore((state) => state.setPeriod);
  const { data: history } = useHistory({
    from: shiftPeriod(period, 1 - LEFTOVER_MONTHS),
    to: period,
    ...(member != null && { member }),
  });
  const rows = (history?.points ?? []).map((point) => ({
    period: point.billingPeriod ?? '',
    label: formatPeriod(point.billingPeriod ?? '', i18n.language).slice(0, 3),
    balance: Number(point.balance ?? 0),
  }));
  const shares = leftoverShares(income, expense);

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="leftover-title">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="leftover-title" className="text-lg font-semibold">
          {t('leftover.title', { period: formatPeriod(period, i18n.language) })}
        </h2>
        <p className="flex items-baseline gap-2">
          <span className="text-sm text-(--color-text-muted)">{balance < 0 ? t('leftover.missing') : t('leftover.left')}</span>
          <MoneyText value={balance} kind="balance" className="text-2xl font-semibold" />
          {shares && <span className="text-sm text-(--color-text-muted)">{t('leftover.ofIncome', { percent: formatPercent(shares.left) })}</span>}
        </p>
      </div>

      {shares ? (
        <div>
          <div
            className="flex h-3 w-full gap-0.5 overflow-hidden rounded-full bg-(--color-surface-muted)"
            role="img"
            aria-label={t('leftover.bar', { spent: formatPercent(shares.spent), left: formatPercent(shares.left) })}
          >
            <span className="h-full rounded-l-full bg-red-600/80" style={{ width: `${shares.spent}%` }} />
            {shares.left > 0 && <span className="h-full rounded-r-full bg-blue-600" style={{ width: `${shares.left}%` }} />}
          </div>
          <p className="mt-1 flex justify-between text-xs text-(--color-text-muted)">
            <span>{t('leftover.spent', { amount: formatMoney(expense), percent: formatPercent(shares.spent) })}</span>
            <span>{t('leftover.income', { amount: formatMoney(income) })}</span>
          </p>
        </div>
      ) : (
        <p className="text-sm text-(--color-text-muted)">{t('leftover.noIncome')}</p>
      )}

      <figure className="h-48 w-full text-(--color-text-muted)" aria-label={t('leftover.chart', { count: LEFTOVER_MONTHS })}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={rows} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} strokeDasharray="3 3" strokeOpacity={0.3} />
            <XAxis dataKey="label" {...axis} />
            <YAxis
              {...axis}
              width={56}
              tickFormatter={(value: number) => new Intl.NumberFormat(i18n.language, { notation: 'compact', maximumFractionDigits: 1 }).format(value)}
            />
            <ReferenceLine y={0} stroke="currentColor" />
            <Tooltip
              cursor={{ fillOpacity: 0.1 }}
              formatter={(value) => [formatMoney(Number(value)), t('leftover.left')]}
              labelFormatter={(_, payload) => formatPeriod(String(payload?.[0]?.payload?.period ?? ''), i18n.language)}
            />
            <Bar
              dataKey="balance"
              isAnimationActive={false}
              maxBarSize={48}
              radius={[4, 4, 0, 0]}
              className="cursor-pointer"
              onClick={(_, index) => {
                const row = rows[index];
                if (row) setPeriod(row.period);
              }}
            >
              {rows.map((row) => (
                <Cell
                  key={row.period}
                  fill={row.balance < 0 ? NEGATIVE : POSITIVE}
                  fillOpacity={row.period === period ? 1 : FADED}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </figure>
    </section>
  );
}

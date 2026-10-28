import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { formatMoney } from '../../shared/format';
import i18n from '../../i18n/i18n';

/** One series of a chart. */
export interface Series {
  /** Data key. */
  key: string;
  /** Legend name. */
  name: string;
  /** Colour. */
  color: string;
}

/** One row of chart data: the bucket label plus one number per series. */
export type ChartRow = { label: string } & Record<string, number | string | null>;

const compact = (value: number) => new Intl.NumberFormat(i18n.language, { notation: 'compact', maximumFractionDigits: 1 }).format(value);
const money = (value: unknown) => formatMoney(Number(value));
const axis = { tick: { fill: 'currentColor', fontSize: 12 }, stroke: 'currentColor' };

function ChartFrame({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="panel flex flex-col gap-2">
      <h2 className="font-medium">{title}</h2>
      <figure className="h-64 w-full text-(--color-text-muted)" aria-label={title}>
        <ResponsiveContainer width="100%" height="100%">
          {children as never}
        </ResponsiveContainer>
      </figure>
    </section>
  );
}

/**
 * Income and expense bars with the balance as a line, per bucket.
 *
 * @param props - chart rows
 * @returns the chart
 */
export function IncomeExpenseChart({ rows }: { rows: ChartRow[] }) {
  const { t } = useTranslation();
  return (
    <ChartFrame title={t('history.incomeVsExpense')}>
      <ComposedChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.3} />
        <XAxis dataKey="label" {...axis} />
        <YAxis tickFormatter={compact} {...axis} width={48} />
        <Tooltip formatter={money} />
        <Legend itemSorter={null} />
        <ReferenceLine y={0} stroke="currentColor" />
        <Bar dataKey="income" name={t('overview.income_short')} fill="#16a34a" isAnimationActive={false} />
        <Bar dataKey="expense" name={t('overview.expense_short')} fill="#dc2626" isAnimationActive={false} />
        <Line dataKey="balance" name={t('overview.balance_short')} stroke="#2563eb" strokeWidth={2} isAnimationActive={false} />
      </ComposedChart>
    </ChartFrame>
  );
}

/**
 * Stacked expense bars per bucket, one stack part per series.
 *
 * @param props - title, rows and series
 * @returns the chart
 */
export function StackedChart({ title, rows, series }: { title: string; rows: ChartRow[]; series: Series[] }) {
  return (
    <ChartFrame title={title}>
      <BarChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.3} />
        <XAxis dataKey="label" {...axis} />
        <YAxis tickFormatter={compact} {...axis} width={48} />
        <Tooltip formatter={money} />
        <Legend itemSorter={null} />
        {series.map((s) => (
          <Bar key={s.key} dataKey={s.key} name={s.name} stackId="expense" fill={s.color} isAnimationActive={false} />
        ))}
      </BarChart>
    </ChartFrame>
  );
}

/**
 * Share of the income spent per bucket, with the alert thresholds (RF-16).
 *
 * @param props - rows and thresholds
 * @returns the chart
 */
export function PercentSpentChart({ rows, warning, danger }: { rows: ChartRow[]; warning: number; danger: number }) {
  const { t } = useTranslation();
  return (
    <ChartFrame title={t('history.percentSpent')}>
      <LineChart data={rows}>
        <CartesianGrid strokeDasharray="3 3" strokeOpacity={0.3} />
        <XAxis dataKey="label" {...axis} />
        <YAxis unit="%" domain={[0, (max: number) => Math.ceil(Math.max(max, danger + 10) / 10) * 10]} {...axis} width={48} />
        <Tooltip formatter={(value) => `${Number(value).toFixed(1)}%`} />
        <ReferenceLine y={warning} stroke="#d97706" strokeDasharray="4 4" label={{ value: t('history.warning'), fill: '#d97706', fontSize: 11 }} />
        <ReferenceLine y={danger} stroke="#dc2626" strokeDasharray="4 4" label={{ value: t('history.danger'), fill: '#dc2626', fontSize: 11 }} />
        <Line dataKey="percentSpent" name={t('history.percentSpent')} stroke="#7c3aed" strokeWidth={2} connectNulls isAnimationActive={false} />
      </LineChart>
    </ChartFrame>
  );
}

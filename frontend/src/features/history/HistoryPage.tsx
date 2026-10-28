import { keepPreviousData } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useHistory } from '../../api/generated/endpoints/history-controller/history-controller';
import type { CategoryModel } from '../../api/generated/model';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { SummaryCard } from '../../shared/components/SummaryCard';
import { formatMoney, formatPercent } from '../../shared/format';
import { useCategoryMap, useMemberMap } from '../../shared/hooks/useLookups';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { useSettingsMap } from '../settings/GeneralSettings';
import { aggregate, type Bucket, type Granularity, presetRange, type RangePreset, topKeys, total } from './aggregate';
import { type ChartRow, IncomeExpenseChart, PercentSpentChart, type Series, StackedChart } from './HistoryCharts';

const GRANULARITIES: Granularity[] = ['month', 'quarter', 'semester', 'year'];
const PRESETS: RangePreset[] = ['thisYear', 'lastYear', 'last12', 'last24', 'last60', 'custom'];
const PALETTE = ['#2563eb', '#db2777', '#16a34a', '#f59e0b', '#7c3aed', '#0891b2', '#dc2626', '#65a30d', '#ea580c'];
const UNCLASSIFIED_COLOR = '#9aa4af';
const OTHER_COLOR = '#64748b';
const TOP = 8;
const MAX_MONTHS = 120;

const monthIndex = (period: string) => {
  const [year, month] = period.split('-').map(Number) as [number, number];
  return year * 12 + month - 1;
};

/**
 * Rolls the expense by category up to what the chart shows: top-level categories without a category filter,
 * or the categories inside the filtered one.
 *
 * @param byCategory - expense by category id
 * @param categories - categories by id
 * @param filter - category filter, or `null`
 * @returns expense by shown key
 */
function rollUp(byCategory: Record<string, number>, categories: Map<number, CategoryModel>, filter: number | null) {
  const result: Record<string, number> = {};
  const ancestors = (id: number) => {
    const chain: number[] = [];
    for (let current = categories.get(id); current?.id != null; current = categories.get(current.parentId ?? -1)) {
      chain.unshift(current.id);
      if (current.parentId == null) break;
    }
    return chain;
  };
  for (const [key, amount] of Object.entries(byCategory)) {
    let shown: string | null;
    if (key === 'none') {
      shown = filter == null || categories.get(filter)?.system ? 'none' : null;
    } else if (filter == null) {
      shown = String(ancestors(Number(key))[0] ?? key);
    } else {
      shown = ancestors(Number(key)).includes(filter) ? key : null;
    }
    if (shown != null) result[shown] = (result[shown] ?? 0) + amount;
  }
  return result;
}

/**
 * History tab: past months grouped by month, quarter, semester or year, with income against expense,
 * expense by source, by category and by member, the share of income spent and a table of totals. Member and
 * category filters narrow the totals; the by-member chart follows the category filter and the by-category
 * chart follows the member filter (RF-10 to RF-16 over time).
 *
 * @returns the page
 */
export function HistoryPage() {
  const { t, i18n } = useTranslation();
  const reference = useBillingPeriod();
  const [granularity, setGranularity] = useState<Granularity>('month');
  const [preset, setPreset] = useState<RangePreset>('last12');
  const [custom, setCustom] = useState(() => presetRange('last12', reference));
  const [member, setMember] = useState<number | null>(null);
  const [category, setCategory] = useState<number | null>(null);
  const categories = useCategoryMap();
  const members = useMemberMap();
  const settings = useSettingsMap();

  const range = preset === 'custom' ? custom : presetRange(preset, reference);
  const span = range.from && range.to ? monthIndex(range.to) - monthIndex(range.from) : -1;
  const valid = span >= 0 && span < MAX_MONTHS;
  const { data } = useHistory(
    { from: range.from, to: range.to, ...(member == null ? {} : { member }), ...(category == null ? {} : { category }) },
    { query: { enabled: valid, placeholderData: keepPreviousData } },
  );

  const buckets = useMemo(() => aggregate(data?.points ?? [], granularity), [data, granularity]);
  const sum = total(buckets);

  const label = (bucket: Bucket) => {
    const [year, part] = bucket.key.split('-');
    switch (granularity) {
      case 'month':
        return new Intl.DateTimeFormat(i18n.language, { month: 'short', timeZone: 'UTC' })
          .format(new Date(Date.UTC(Number(year), Number(part) - 1, 1)))
          .replace('.', '') + `/${year!.slice(2)}`;
      case 'quarter':
        return t('history.quarterLabel', { n: part!.slice(1), year });
      case 'semester':
        return t('history.semesterLabel', { n: part!.slice(1), year });
      case 'year':
        return year!;
    }
  };

  const categoryName = (key: string) =>
    key === 'none' ? t('overview.unclassified') : (categories.get(Number(key))?.name ?? key);
  const memberName = (key: string) => {
    const m = members.get(Number(key));
    return m?.name ?? m?.code ?? key;
  };

  const categoryBuckets = buckets.map((b) => ({ ...b, byCategory: rollUp(b.byCategory, categories, category) }));
  const categoryTop = topKeys(categoryBuckets, (b) => b.byCategory, TOP);
  const memberTop = topKeys(buckets, (b) => b.byMember, TOP);

  const breakdownRows = (source: Bucket[], pick: (b: Bucket) => Record<string, number>, keys: string[]): ChartRow[] =>
    source.map((b) => {
      const row: ChartRow = { label: label(b) };
      let other = 0;
      for (const [key, amount] of Object.entries(pick(b))) {
        if (keys.includes(key)) row[key] = amount;
        else other += amount;
      }
      if (other !== 0) row.other = Math.round(other * 100) / 100;
      return row;
    });
  const withOther = (series: Series[], totals: [string, number][]) =>
    totals.length > series.length ? [...series, { key: 'other', name: t('history.other'), color: OTHER_COLOR }] : series;

  const rows: ChartRow[] = buckets.map((b) => ({
    label: label(b),
    income: b.income,
    expense: b.expense,
    balance: b.balance,
    fixed: b.fixed,
    variable: b.variable,
    card: b.card,
    percentSpent: b.percentSpent,
  }));
  const categorySeries = withOther(
    categoryTop.keys.map((key, i) => ({
      key,
      name: categoryName(key),
      color: key === 'none' ? UNCLASSIFIED_COLOR : (categories.get(Number(key))?.color ?? PALETTE[i % PALETTE.length]!),
    })),
    categoryTop.totals,
  );
  const memberSeries = withOther(
    memberTop.keys.map((key, i) => ({ key, name: memberName(key), color: PALETTE[i % PALETTE.length]! })),
    memberTop.totals,
  );
  const sourceSeries: Series[] = [
    { key: 'fixed', name: t('overview.fixed'), color: '#f59e0b' },
    { key: 'variable', name: t('overview.variable'), color: '#8b5cf6' },
    { key: 'card', name: t('overview.card'), color: '#0ea5e9' },
  ];
  const share = (amount: number, all: [string, number][]) => {
    const whole = all.reduce((n, [, value]) => n + value, 0);
    return whole > 0 ? formatPercent((amount / whole) * 100) : '—';
  };

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">{t('nav.history')}</h1>

      <div className="panel flex flex-wrap items-end gap-3">
        <Field label={t('history.granularity')} htmlFor="history-granularity">
          <select id="history-granularity" className="input" value={granularity} onChange={(e) => setGranularity(e.target.value as Granularity)}>
            {GRANULARITIES.map((g) => (
              <option key={g} value={g}>
                {t(`history.${g}`)}
              </option>
            ))}
          </select>
        </Field>
        <Field label={t('history.range')} htmlFor="history-range">
          <select
            id="history-range"
            className="input"
            value={preset}
            onChange={(e) => {
              const next = e.target.value as RangePreset;
              if (next === 'custom' && preset !== 'custom') setCustom(presetRange(preset, reference));
              setPreset(next);
            }}
          >
            {PRESETS.map((p) => (
              <option key={p} value={p}>
                {t(`history.${p}`)}
              </option>
            ))}
          </select>
        </Field>
        {preset === 'custom' && (
          <>
            <Field label={t('history.from')} htmlFor="history-from">
              <input id="history-from" type="month" className="input" value={custom.from} onChange={(e) => setCustom({ ...custom, from: e.target.value })} />
            </Field>
            <Field label={t('history.to')} htmlFor="history-to">
              <input id="history-to" type="month" className="input" value={custom.to} onChange={(e) => setCustom({ ...custom, to: e.target.value })} />
            </Field>
          </>
        )}
        <Field label={t('history.memberFilter')} htmlFor="history-member">
          <MemberPicker id="history-member" allowEmpty emptyLabel={t('history.everyone')} includeInactive value={member} onChange={setMember} />
        </Field>
        <Field label={t('history.categoryFilter')} htmlFor="history-category">
          <CategoryPicker id="history-category" allowEmpty emptyLabel={t('history.allCategories')} value={category} onChange={setCategory} />
        </Field>
        <p className="basis-full text-xs text-(--color-text-muted)">{t('history.filterHint')}</p>
      </div>

      {!valid && (
        <p role="alert" className="panel text-red-700">
          {t('history.invalidRange')}
        </p>
      )}

      <div className="grid gap-3 sm:grid-cols-2 wide:grid-cols-4">
        <SummaryCard title={t('overview.income')} value={<MoneyText value={sum.income} />} />
        <SummaryCard title={t('overview.expense')} value={<MoneyText value={sum.expense} />} />
        <SummaryCard title={t('overview.balance')} value={<MoneyText value={sum.balance} kind="balance" />} />
        <SummaryCard
          title={t('history.average')}
          value={<MoneyText value={sum.months > 0 ? Math.round((sum.expense / sum.months) * 100) / 100 : 0} />}
          hint={sum.percentSpent == null ? undefined : `${t('overview.spent')}: ${formatPercent(sum.percentSpent)}`}
        />
      </div>

      <div className="grid gap-4 wide:grid-cols-2">
        <IncomeExpenseChart rows={rows} />
        <StackedChart title={t('history.bySource')} rows={rows} series={sourceSeries} />
        <StackedChart
          title={t('history.byCategory')}
          rows={breakdownRows(categoryBuckets, (b) => b.byCategory, categoryTop.keys)}
          series={categorySeries}
        />
        <StackedChart title={t('history.byMember')} rows={breakdownRows(buckets, (b) => b.byMember, memberTop.keys)} series={memberSeries} />
        <PercentSpentChart
          rows={rows}
          warning={Number(settings['alert-warning-percent'] ?? '80')}
          danger={Number(settings['alert-danger-percent'] ?? '100')}
        />
      </div>

      <div className="grid gap-4 wide:grid-cols-2">
        <TotalsTable
          title={t('history.totalsByCategory')}
          head={t('overview.category')}
          rows={categoryTop.totals.map(([key, amount]) => ({ key, name: categoryName(key), amount, share: share(amount, categoryTop.totals) }))}
          emptyText={t('history.empty')}
        />
        <TotalsTable
          title={t('history.totalsByMember')}
          head={t('overview.member')}
          rows={memberTop.totals.map(([key, amount]) => ({ key, name: memberName(key), amount, share: share(amount, memberTop.totals) }))}
          emptyText={t('history.empty')}
        />
      </div>

      <section className="panel flex flex-col gap-2">
        <h2 className="font-medium">{t('history.table')}</h2>
        <div className="overflow-x-auto">
          <table className="table">
            <caption className="sr-only">{t('history.table')}</caption>
            <thead>
              <tr>
                <th scope="col">{t('history.period')}</th>
                <th scope="col" className="text-right">{t('overview.income_short')}</th>
                <th scope="col" className="hidden text-right wide:table-cell">{t('overview.fixed')}</th>
                <th scope="col" className="hidden text-right wide:table-cell">{t('overview.variable')}</th>
                <th scope="col" className="hidden text-right wide:table-cell">{t('overview.card')}</th>
                <th scope="col" className="text-right">{t('overview.expense_short')}</th>
                <th scope="col" className="text-right">{t('overview.balance_short')}</th>
                <th scope="col" className="text-right">{t('overview.spent')}</th>
              </tr>
            </thead>
            <tbody>
              {[...buckets, sum].map((b) => (
                <tr key={b.key} className={b.key === 'total' ? 'font-semibold' : undefined}>
                  <th scope="row" className="font-normal">{b.key === 'total' ? t('history.total') : label(b)}</th>
                  <td className="text-right">{formatMoney(b.income)}</td>
                  <td className="hidden text-right wide:table-cell">{formatMoney(b.fixed)}</td>
                  <td className="hidden text-right wide:table-cell">{formatMoney(b.variable)}</td>
                  <td className="hidden text-right wide:table-cell">{formatMoney(b.card)}</td>
                  <td className="text-right">{formatMoney(b.expense)}</td>
                  <td className="text-right">
                    <MoneyText value={b.balance} kind="balance" />
                  </td>
                  <td className="text-right">{b.percentSpent == null ? '—' : formatPercent(b.percentSpent)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function TotalsTable({ title, head, rows, emptyText }: {
  title: string;
  head: string;
  rows: { key: string; name: string; amount: number; share: string }[];
  emptyText: string;
}) {
  const { t } = useTranslation();
  return (
    <section className="panel flex flex-col gap-2">
      <h2 className="font-medium">{title}</h2>
      {rows.length === 0 ? (
        <p className="text-sm text-(--color-text-muted)">{emptyText}</p>
      ) : (
        <table className="table">
          <caption className="sr-only">{title}</caption>
          <thead>
            <tr>
              <th scope="col">{head}</th>
              <th scope="col" className="text-right">{t('overview.expense_short')}</th>
              <th scope="col" className="text-right">{t('overview.share')}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.key}>
                <th scope="row" className="font-normal">{row.name}</th>
                <td className="text-right">{formatMoney(row.amount)}</td>
                <td className="text-right">{row.share}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

import type { HistoryMonth } from '../../api/generated/model';

/** Size of one bar of the history charts. */
export type Granularity = 'month' | 'quarter' | 'semester' | 'year';

/** Ready-made ranges of the history tab. */
export type RangePreset = 'thisYear' | 'lastYear' | 'last12' | 'last24' | 'last60' | 'custom';

/** Totals of one month, quarter, semester or year. */
export interface Bucket {
  /** `2026-11`, `2026-Q4`, `2026-S2` or `2026`. */
  key: string;
  /** Number of months in the bucket. */
  months: number;
  income: number;
  mainIncome: number;
  reimbursements: number;
  expense: number;
  fixed: number;
  variable: number;
  card: number;
  /** income − expense. */
  balance: number;
  /** expense / income × 100, or `null` without income. */
  percentSpent: number | null;
  /** Expense by category id (`none` for not classified). */
  byCategory: Record<string, number>;
  /** Expense by member id. */
  byMember: Record<string, number>;
}

const SUMS = ['income', 'mainIncome', 'reimbursements', 'expense', 'fixed', 'variable', 'card'] as const;

const cents = (value: number) => Math.round(value * 100) / 100;

/**
 * Returns the bucket a billing period falls in.
 *
 * @param period - billing period, `yyyy-MM`
 * @param granularity - bucket size
 * @returns bucket key
 */
export function bucketKey(period: string, granularity: Granularity): string {
  const [year, month] = period.split('-').map(Number) as [number, number];
  switch (granularity) {
    case 'month':
      return period;
    case 'quarter':
      return `${year}-Q${Math.ceil(month / 3)}`;
    case 'semester':
      return `${year}-S${month <= 6 ? 1 : 2}`;
    case 'year':
      return String(year);
  }
}

/**
 * Groups the months of the history into buckets, adding every amount and the breakdowns, oldest first.
 *
 * @param points - months as sent by the API
 * @param granularity - bucket size
 * @returns one bucket per month, quarter, semester or year that has a month in the range
 */
export function aggregate(points: HistoryMonth[], granularity: Granularity): Bucket[] {
  const buckets = new Map<string, Bucket>();
  for (const point of points) {
    const key = bucketKey(point.billingPeriod ?? '', granularity);
    let bucket = buckets.get(key);
    if (!bucket) {
      bucket = { key, months: 0, income: 0, mainIncome: 0, reimbursements: 0, expense: 0, fixed: 0, variable: 0, card: 0,
        balance: 0, percentSpent: null, byCategory: {}, byMember: {} };
      buckets.set(key, bucket);
    }
    bucket.months += 1;
    for (const field of SUMS) bucket[field] = cents(bucket[field] + (point[field] ?? 0));
    for (const [id, amount] of Object.entries(point.byCategory ?? {})) {
      bucket.byCategory[id] = cents((bucket.byCategory[id] ?? 0) + amount);
    }
    for (const [id, amount] of Object.entries(point.byMember ?? {})) {
      bucket.byMember[id] = cents((bucket.byMember[id] ?? 0) + amount);
    }
  }
  return [...buckets.values()].map((bucket) => ({
    ...bucket,
    balance: cents(bucket.income - bucket.expense),
    percentSpent: bucket.income > 0 ? Math.round((bucket.expense / bucket.income) * 1000) / 10 : null,
  }));
}

/**
 * Adds the buckets into one total for the whole range.
 *
 * @param buckets - buckets of the range
 * @returns the total, keyed `total`
 */
export function total(buckets: Bucket[]): Bucket {
  const months = [{ billingPeriod: '0000-01' }, ...buckets.map((bucket) => ({ ...bucket, billingPeriod: '0000-01' }))];
  return { ...aggregate(months, 'year')[0]!, key: 'total', months: buckets.reduce((n, bucket) => n + bucket.months, 0) };
}

/**
 * Turns a ready-made range into periods, relative to a reference month.
 *
 * @param preset - range name
 * @param reference - reference month, `yyyy-MM`
 * @returns first and last period
 */
export function presetRange(preset: Exclude<RangePreset, 'custom'>, reference: string): { from: string; to: string } {
  const [year, month] = reference.split('-').map(Number) as [number, number];
  const shift = (months: number) => {
    const index = year * 12 + (month - 1) + months;
    return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`;
  };
  switch (preset) {
    case 'thisYear':
      return { from: `${year}-01`, to: reference };
    case 'lastYear':
      return { from: `${year - 1}-01`, to: `${year - 1}-12` };
    case 'last12':
      return { from: shift(-11), to: reference };
    case 'last24':
      return { from: shift(-23), to: reference };
    case 'last60':
      return { from: shift(-59), to: reference };
  }
}

/**
 * Sums a breakdown over the buckets and keeps the largest keys, the rest going into `other`.
 *
 * @param buckets - buckets of the range
 * @param pick - breakdown of one bucket
 * @param limit - how many keys to keep
 * @returns kept keys, largest first, and the totals of every key
 */
export function topKeys(buckets: Bucket[], pick: (b: Bucket) => Record<string, number>, limit: number) {
  const totals = new Map<string, number>();
  for (const bucket of buckets) {
    for (const [key, amount] of Object.entries(pick(bucket))) totals.set(key, cents((totals.get(key) ?? 0) + amount));
  }
  const ordered = [...totals.entries()].filter(([, amount]) => amount !== 0).sort((a, b) => b[1] - a[1]);
  return { keys: ordered.slice(0, limit).map(([key]) => key), totals: ordered };
}

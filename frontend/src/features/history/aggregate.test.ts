import { describe, expect, it } from 'vitest';
import { aggregate, bucketKey, presetRange, topKeys, total } from './aggregate';

const month = (billingPeriod: string, income: number, expense: number, byCategory = {}, byMember = {}) => ({
  billingPeriod,
  income,
  mainIncome: income,
  reimbursements: 0,
  expense,
  fixed: expense,
  variable: 0,
  card: 0,
  balance: income - expense,
  byCategory,
  byMember,
});

describe('history aggregation', () => {
  it('RF-10: periods fall in month, quarter, semester and year buckets', () => {
    expect(bucketKey('2026-11', 'month')).toBe('2026-11');
    expect(bucketKey('2026-11', 'quarter')).toBe('2026-Q4');
    expect(bucketKey('2026-03', 'quarter')).toBe('2026-Q1');
    expect(bucketKey('2026-06', 'semester')).toBe('2026-S1');
    expect(bucketKey('2026-07', 'semester')).toBe('2026-S2');
    expect(bucketKey('2026-07', 'year')).toBe('2026');
  });

  it('RF-10, RF-15: buckets add amounts and breakdowns and recompute balance and % spent', () => {
    const [s1, s2] = aggregate(
      [
        month('2026-05', 1000, 400.1, { '5': 400.1 }, { '1': 400.1 }),
        month('2026-06', 1000, 600.2, { '5': 100, none: 500.2 }, { '1': 600.2 }),
        month('2026-07', 0, 50, {}, { '2': 50 }),
      ],
      'semester',
    );
    expect(s1).toMatchObject({ key: '2026-S1', months: 2, income: 2000, expense: 1000.3, balance: 999.7, percentSpent: 50 });
    expect(s1!.byCategory).toEqual({ '5': 500.1, none: 500.2 });
    expect(s2).toMatchObject({ key: '2026-S2', percentSpent: null, balance: -50, byMember: { '2': 50 } });
    expect(total([s1!, s2!])).toMatchObject({ key: 'total', months: 3, income: 2000, expense: 1050.3, balance: 949.7 });
    expect(total([])).toMatchObject({ months: 0, expense: 0 });
  });

  it('RF-15: ready-made ranges end at the reference month', () => {
    expect(presetRange('thisYear', '2026-10')).toEqual({ from: '2026-01', to: '2026-10' });
    expect(presetRange('lastYear', '2026-10')).toEqual({ from: '2025-01', to: '2025-12' });
    expect(presetRange('last12', '2026-10')).toEqual({ from: '2025-11', to: '2026-10' });
    expect(presetRange('last24', '2026-01')).toEqual({ from: '2024-02', to: '2026-01' });
    expect(presetRange('last60', '2026-10')).toEqual({ from: '2021-11', to: '2026-10' });
  });

  it('RF-12: the largest keys are kept, largest first', () => {
    const buckets = aggregate([month('2026-01', 0, 0, { a: 1, b: 5, c: 3, z: 0 })], 'month');
    expect(topKeys(buckets, (b) => b.byCategory, 2).keys).toEqual(['b', 'c']);
    expect(topKeys(buckets, (b) => b.byCategory, 2).totals).toHaveLength(3);
  });
});

import { describe, expect, it } from 'vitest';
import { categoryEntriesPath } from './categoryEntriesPath';

describe('categoryEntriesPath', () => {
  it('RF-12, RF-38: links the period and the category, and the member only when filtered', () => {
    expect(categoryEntriesPath('2026-11', 5, null)).toBe('/entries?billingPeriod=2026-11&category=5');
    expect(categoryEntriesPath('2026-11', 5, 2)).toBe('/entries?billingPeriod=2026-11&category=5&member=2');
  });
});

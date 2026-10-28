import { describe, expect, it } from 'vitest';
import { chartHeight } from './CategoryChart';

describe('chartHeight', () => {
  it('UI-07: grows with the number of categories within its bounds', () => {
    expect(chartHeight(0)).toBe(224);
    expect(chartHeight(3)).toBe(244);
    expect(chartHeight(6)).toBeGreaterThan(chartHeight(3));
    expect(chartHeight(40)).toBe(420);
  });
});

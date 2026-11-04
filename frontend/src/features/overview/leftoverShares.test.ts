import { describe, expect, it } from 'vitest';
import { leftoverShares } from './LeftoverChart';

describe('leftoverShares', () => {
  it('RF-10: splits the income into what was spent and what was left', () => {
    expect(leftoverShares(10000, 7500)).toEqual({ spent: 75, left: 25 });
  });

  it('RF-10: spending above the income fills the bar and leaves nothing', () => {
    expect(leftoverShares(1000, 1500)).toEqual({ spent: 100, left: 0 });
  });

  it('RF-10: without income there is nothing to measure against', () => {
    expect(leftoverShares(0, 100)).toBeNull();
  });
});

import { describe, expect, it } from 'vitest';
import { selectRows } from './selection';

const ids = [1, 2, 3, 4, 5];

describe('selectRows', () => {
  it('UI-04: plain click selects only the clicked row', () => {
    expect([...selectRows(new Set([1, 2]), ids, 3, 1, { ctrl: false, shift: false })]).toEqual([3]);
  });

  it('UI-04: Ctrl+click toggles one row', () => {
    expect([...selectRows(new Set([1]), ids, 3, 1, { ctrl: true, shift: false })].sort()).toEqual([1, 3]);
    expect([...selectRows(new Set([1, 3]), ids, 3, 1, { ctrl: true, shift: false })]).toEqual([1]);
  });

  it('UI-04: Shift+click selects the range from the last clicked row, in both directions', () => {
    expect([...selectRows(new Set(), ids, 4, 2, { ctrl: false, shift: true })].sort()).toEqual([2, 3, 4]);
    expect([...selectRows(new Set([5]), ids, 1, 3, { ctrl: false, shift: true })].sort()).toEqual([1, 2, 3, 5]);
    expect([...selectRows(new Set(), ids, 4, null, { ctrl: false, shift: true })]).toEqual([4]);
  });
});

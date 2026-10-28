import { describe, expect, it } from 'vitest';
import { addShare, convertSplit, remainderOf, sharesOf, splitOf, type Split } from './split';

const byPercent: Split = { mode: 'percentage', shares: [{ memberId: 1, value: 100 }] };

describe('split', () => {
  it('RF-26: reads shares by amount as amounts and anything else as percentages', () => {
    expect(splitOf([{ memberId: 1, amount: 30 }, { memberId: 2, amount: 70 }])).toEqual({
      mode: 'amount', shares: [{ memberId: 1, value: 30 }, { memberId: 2, value: 70 }],
    });
    expect(splitOf([{ memberId: 1, percentage: 40 }, { memberId: 2, amount: 10 }]).mode).toBe('percentage');
    expect(splitOf([{ memberId: 1 }])).toEqual(byPercent);
  });

  it('RF-26: sends either percentages or non-negative amounts, without empty rows', () => {
    expect(sharesOf({ mode: 'amount', shares: [{ memberId: 1, value: -30 }, { memberId: null, value: 5 }] })).toEqual([
      { memberId: 1, amount: 30 },
    ]);
    expect(sharesOf(byPercent)).toEqual([{ memberId: 1, percentage: 100 }]);
  });

  it('RF-26: adding a member divides the whole evenly, the last one taking what is left', () => {
    expect(addShare(addShare(byPercent, 100), 100).shares.map((s) => s.value)).toEqual([33.33, 33.33, 33.34]);
    const amounts = addShare({ mode: 'amount', shares: [{ memberId: 1, value: 10 }] }, -10);
    expect(amounts.shares).toEqual([{ memberId: 1, value: 5 }, { memberId: null, value: 5 }]);
  });

  it('RF-26: switching mode keeps each member part of the whole, to the cent', () => {
    const split: Split = { mode: 'percentage', shares: [{ memberId: 1, value: 30 }, { memberId: 2, value: 70 }] };
    const amounts = convertSplit(split, 'amount', 99.99);
    expect(amounts.shares.map((s) => s.value)).toEqual([29.99, 70]);
    expect(convertSplit(amounts, 'percentage', 99.99).shares.map((s) => s.value)).toEqual([29.99, 70.01]);
    expect(convertSplit(split, 'percentage', 10)).toBe(split);
  });

  it('RF-26: the remainder says what is missing or over', () => {
    expect(remainderOf({ mode: 'percentage', shares: [{ memberId: 1, value: 30 }, { memberId: 2, value: 50 }] }, 10)).toBe(20);
    expect(remainderOf({ mode: 'amount', shares: [{ memberId: 1, value: 30 }, { memberId: 2, value: 80 }] }, -100)).toBe(-10);
    expect(remainderOf(byPercent, 10)).toBe(0);
  });
});

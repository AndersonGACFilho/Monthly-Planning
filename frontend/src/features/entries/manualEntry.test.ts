import { describe, expect, it } from 'vitest';
import type { BillEntryModel } from '../../api/generated/model';
import { draftOf, requestOf } from './manualEntry';

const entry: BillEntryModel = {
  id: 9, cardId: 7, purchaseDate: '2026-10-05', originalDescription: 'Geladeira', amount: 250, installment: '03/04',
  billingPeriod: '2027-01', categoryId: 5, splits: [{ memberId: 2, percentage: 100 }],
};

describe('manual entry draft', () => {
  it('RF-44: a new entry starts on the period on screen with one installment', () => {
    expect(draftOf('2026-11')).toMatchObject({ billingPeriod: '2026-11', installmentCount: 1, cardId: null, amount: null });
  });

  it('RF-44, RF-45: editing any installment starts on the bill of the first one', () => {
    expect(draftOf('2026-11', entry)).toEqual({
      cardId: 7, purchaseDate: '2026-10-05', description: 'Geladeira', amount: 250, installmentCount: 4, memberId: 2,
      categoryId: 5, billingPeriod: '2026-11',
    });
    expect(draftOf('2026-11', { ...entry, installment: '-', billingPeriod: '2026-12' })).toMatchObject({
      installmentCount: 1, billingPeriod: '2026-12',
    });
  });

  it('RF-43: a split between members leaves the member empty, so the backend keeps it', () => {
    const split = { ...entry, splits: [{ memberId: 1, percentage: 50 }, { memberId: 2, percentage: 50 }] };
    expect(draftOf('2026-11', split).memberId).toBeNull();
  });

  it('RF-44: the request needs a card and an amount, and an empty installment count means one', () => {
    const draft = draftOf('2026-11', entry);
    expect(requestOf({ ...draft, amount: null })).toBeNull();
    expect(requestOf({ ...draft, installmentCount: 0, memberId: null })).toEqual({
      cardId: 7, purchaseDate: '2026-10-05', description: 'Geladeira', amount: 250, installmentCount: 1, categoryId: 5,
      billingPeriod: '2026-11',
    });
  });
});

import type { BillEntryModel, ManualEntryRequest } from '../../api/generated/model';
import { isoDateOf, shiftPeriod } from '../../shared/format';

/** What the manual entry form edits. */
export interface ManualEntryDraft {
  cardId: number | null;
  purchaseDate: string;
  description: string;
  amount: number | null;
  installmentCount: number;
  memberId: number | null;
  categoryId: number | null;
  billingPeriod: string;
}

/** Installment label such as `03/10`. */
const INSTALLMENT = /^(\d+)\/(\d+)$/;

/**
 * Starts the form: empty on the period on screen, or filled with the purchase of an entry. An edit starts on
 * the bill of the first installment, whatever installment was opened; a split between members leaves the
 * member empty, so the split is kept.
 *
 * @param period - period on screen
 * @param entry  - entry being edited, if any
 * @returns the draft
 */
export function draftOf(period: string, entry?: BillEntryModel): ManualEntryDraft {
  if (!entry) {
    return {
      cardId: null, purchaseDate: isoDateOf(), description: '', amount: null, installmentCount: 1, memberId: null,
      categoryId: null, billingPeriod: period,
    };
  }
  const [, number = '1', count = '1'] = INSTALLMENT.exec(entry.installment ?? '') ?? [];
  const splits = entry.splits ?? [];
  return {
    cardId: entry.cardId ?? null,
    purchaseDate: entry.purchaseDate ?? isoDateOf(),
    description: entry.originalDescription ?? '',
    amount: entry.amount ?? null,
    installmentCount: Number(count),
    memberId: splits.length === 1 ? (splits[0]?.memberId ?? null) : null,
    categoryId: entry.categoryId ?? null,
    billingPeriod: shiftPeriod(entry.billingPeriod ?? period, 1 - Number(number)),
  };
}

/**
 * Builds the request of a complete draft.
 *
 * @param draft - form data
 * @returns the request, or `null` while the card or the amount is missing
 */
export function requestOf(draft: ManualEntryDraft): ManualEntryRequest | null {
  if (draft.cardId == null || draft.amount == null) return null;
  return {
    cardId: draft.cardId,
    purchaseDate: draft.purchaseDate,
    description: draft.description,
    amount: draft.amount,
    installmentCount: Math.max(1, draft.installmentCount || 1),
    memberId: draft.memberId ?? undefined,
    categoryId: draft.categoryId ?? undefined,
    billingPeriod: draft.billingPeriod || undefined,
  };
}

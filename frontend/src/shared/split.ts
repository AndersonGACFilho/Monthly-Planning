import type { Share, ShareRequest } from '../api/generated/model';
import { fromCents, toCents } from './format';

/** How the members' shares are given: percentages of the amount, or amounts per installment (RF-26). */
export type SplitMode = 'percentage' | 'amount';

/** Share of one member: a percentage or an amount, depending on the {@link Split} mode. */
export interface SplitShare {
  memberId: number | null;
  value: number;
}

/** Member shares of a purchase, all given the same way. */
export interface Split {
  mode: SplitMode;
  shares: SplitShare[];
}

/** Percentages are handled in hundredths, so 33.33 + 33.33 + 33.34 adds up exactly. */
const PERCENT_UNITS = 100 * 100;

/**
 * Divides whole units in proportion to weights, rounding down and giving what is left to the last share.
 *
 * @param units   - units to divide
 * @param weights - weight of each share; all zero divides evenly
 * @returns units of each share, adding up to `units`
 */
function distribute(units: number, weights: number[]): number[] {
  const total = weights.reduce((sum, w) => sum + w, 0);
  const parts = weights.map((w) => Math.floor((units * (total > 0 ? w : 1)) / (total > 0 ? total : weights.length)));
  const given = parts.reduce((sum, p) => sum + p, 0);
  return parts.map((p, i) => (i === parts.length - 1 ? p + units - given : p));
}

/** Units of a whole split in a mode: hundredths of a percent, or cents of the amount. */
function unitsOf(mode: SplitMode, total: number): number {
  return mode === 'percentage' ? PERCENT_UNITS : Math.abs(toCents(total));
}

const toUnits = (mode: SplitMode, value: number) => (mode === 'percentage' ? Math.round(value * 100) : toCents(value));
const fromUnits = (mode: SplitMode, units: number) => (mode === 'percentage' ? units / 100 : fromCents(units));

/**
 * Reads the shares of the API. Shares by amount are kept as amounts; anything else is read as percentages.
 *
 * @param shares - shares of the purchase
 * @returns the split
 */
export function splitOf(shares: Share[]): Split {
  const byAmount = shares.length > 0 && shares.every((s) => s.amount != null && s.percentage == null);
  return {
    mode: byAmount ? 'amount' : 'percentage',
    shares: shares.map((s) => ({ memberId: s.memberId ?? null, value: Number((byAmount ? s.amount : s.percentage) ?? 100) })),
  };
}

/**
 * Builds the shares to send, leaving out rows without a member.
 *
 * @param split - the split
 * @returns shares with either `percentage` or a non-negative `amount`
 */
export function sharesOf(split: Split): ShareRequest[] {
  return split.shares
    .filter((s): s is SplitShare & { memberId: number } => s.memberId != null)
    .map((s) => (split.mode === 'percentage' ? { memberId: s.memberId, percentage: s.value } : { memberId: s.memberId, amount: Math.abs(s.value) }));
}

/**
 * Adds one more member and divides the whole evenly between everyone.
 *
 * @param split - current split
 * @param total - installment amount
 * @returns the split with a new, empty member
 */
export function addShare(split: Split, total: number): Split {
  const members = [...split.shares.map((s) => s.memberId), null];
  const units = distribute(unitsOf(split.mode, total), members.map(() => 1));
  return { mode: split.mode, shares: members.map((memberId, i) => ({ memberId, value: fromUnits(split.mode, units[i]!) })) };
}

/**
 * Changes how the shares are given, keeping each member's part of the whole.
 *
 * @param split - current split
 * @param mode  - new mode
 * @param total - installment amount
 * @returns the converted split
 */
export function convertSplit(split: Split, mode: SplitMode, total: number): Split {
  if (mode === split.mode) return split;
  const units = distribute(unitsOf(mode, total), split.shares.map((s) => Math.max(0, toUnits(split.mode, s.value))));
  return { mode, shares: split.shares.map((s, i) => ({ memberId: s.memberId, value: fromUnits(mode, units[i]!) })) };
}

/**
 * Tells how much of the whole is still unassigned: percentage points, or money of the installment.
 *
 * @param split - the split
 * @param total - installment amount
 * @returns what is missing (positive) or over (negative); `0` when the split is complete
 */
export function remainderOf(split: Split, total: number): number {
  const given = split.shares.reduce((sum, s) => sum + toUnits(split.mode, Number.isFinite(s.value) ? s.value : 0), 0);
  return fromUnits(split.mode, unitsOf(split.mode, total) - given);
}

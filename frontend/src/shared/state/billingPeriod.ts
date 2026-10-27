import { create } from 'zustand';
import { periodOf, shiftPeriod } from '../format';

/** Global billing period selected at the top of the window (UI-02). */
interface BillingPeriodState {
  /** Selected period as `yyyy-MM`. */
  period: string;
  /** Selects a period. */
  setPeriod: (period: string) => void;
  /** Moves one month forward or back. */
  shift: (months: number) => void;
}

/**
 * Store of the selected billing period; every tab reads it (SPEC section 1).
 */
export const useBillingPeriodStore = create<BillingPeriodState>((set) => ({
  period: periodOf(),
  setPeriod: (period) => set({ period }),
  shift: (months) => set((state) => ({ period: shiftPeriod(state.period, months) })),
}));

/**
 * Returns the selected billing period (UI-02).
 *
 * @returns period as `yyyy-MM`
 */
export function useBillingPeriod(): string {
  return useBillingPeriodStore((state) => state.period);
}

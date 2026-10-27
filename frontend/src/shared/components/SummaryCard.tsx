import type { ReactNode } from 'react';

/** Props of {@link SummaryCard}. */
export interface SummaryCardProps {
  /** Title. */
  title: string;
  /** Main value. */
  value: ReactNode;
  /** Optional line under the value. */
  hint?: ReactNode;
  /** Colour of the left border, for alerts (RF-16). */
  tone?: 'neutral' | 'warning' | 'danger' | 'success';
  /** Optional click action, such as opening the card's entries (RF-38). */
  onClick?: () => void;
  /** Extra content. */
  children?: ReactNode;
}

const TONES = {
  neutral: 'border-l-(--color-border)',
  warning: 'border-l-amber-500',
  danger: 'border-l-red-600',
  success: 'border-l-emerald-600',
};

/**
 * Card with a title and a highlighted value, shared by the overview and the card totals (SPEC section 13).
 *
 * @param props - {@link SummaryCardProps}
 * @returns the card
 */
export function SummaryCard({ title, value, hint, tone = 'neutral', onClick, children }: SummaryCardProps) {
  const body = (
    <>
      <p className="text-sm text-(--color-text-muted)">{title}</p>
      <p className="text-2xl font-semibold">{value}</p>
      {hint && <p className="text-sm text-(--color-text-muted)">{hint}</p>}
      {children}
    </>
  );
  const className = `rounded border border-l-4 border-(--color-border) ${TONES[tone]} bg-(--color-surface) p-4 text-left`;
  return onClick ? (
    <button type="button" className={`${className} hover:bg-(--color-surface-muted)`} onClick={onClick}>
      {body}
    </button>
  ) : (
    <section className={className}>{body}</section>
  );
}

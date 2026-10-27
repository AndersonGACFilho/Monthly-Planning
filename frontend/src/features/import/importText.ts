/** Translation keys of import warnings (RF-62, RF-64h, RF-73). */
export const WARNING_KEYS: Record<string, string> = {
  'period-mismatch': 'import.warning.periodMismatch',
  'card-mismatch': 'import.warning.cardMismatch',
  'already-imported': 'import.warning.alreadyImported',
  'totals-mismatch': 'import.warning.totalsMismatch',
};

/** Order in which line groups are shown; divergent lines first and expanded (UI-06). */
export const STATUS_ORDER = ['DIVERGENT', 'NEW', 'MATCHED', 'DUPLICATE'] as const;

/** Why a backup was made, from the tag at the end of its file name (POR-12, POR-14, IMP-58). */
export type BackupReason = 'start' | 'before-update' | 'manual' | 'before-restore' | 'other';

/** A backup file name read back into its parts. */
export interface BackupName {
  /** When the copy was made, or `null` when the name does not follow the app pattern. */
  date: Date | null;
  /** Why it was made. */
  reason: BackupReason;
}

const PATTERN = /^planejamento-mensal-(\d{4})-(\d{2})-(\d{2})T(\d{2})-(\d{2})-(\d{2})-\d{3}-([a-z-]+)\.db$/;
const REASONS: readonly BackupReason[] = ['start', 'before-update', 'manual', 'before-restore'];

/**
 * Reads the date and the reason out of a backup file name such as
 * `planejamento-mensal-2026-11-04T10-16-09-178-start.db`.
 *
 * @param fileName - name of a file in `data/backups/`
 * @returns its date and reason
 */
export function parseBackupName(fileName: string): BackupName {
  const match = PATTERN.exec(fileName);
  if (!match) return { date: null, reason: 'other' };
  const [, y, mo, d, h, mi, s, tag] = match;
  const reason = (REASONS as readonly string[]).includes(tag!) ? (tag as BackupReason) : 'other';
  return { date: new Date(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(s)), reason };
}

/**
 * Describes a backup for the interface, such as "04/11/2026 10:16".
 *
 * @param fileName - name of a file in `data/backups/`
 * @param language - interface language
 * @returns date and time, or the file name when it does not follow the app pattern
 */
export function backupLabel(fileName: string, language: string): string {
  const { date } = parseBackupName(fileName);
  return date ? new Intl.DateTimeFormat(language, { dateStyle: 'short', timeStyle: 'short' }).format(date) : fileName;
}

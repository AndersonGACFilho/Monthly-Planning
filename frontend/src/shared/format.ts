import i18n from '../i18n/i18n';

/**
 * Formats an amount in reais in the interface language (I18N-05, RNF-02): `R$ 1.234,56` in pt-BR and
 * `R$1,234.56` in en-US. The currency is always BRL.
 *
 * @param value - amount as sent by the API
 * @param language - language tag; defaults to the interface language
 * @returns the formatted amount
 */
export function formatMoney(value: number | string | null | undefined, language: string = i18n.language): string {
  const amount = typeof value === 'string' ? Number(value) : (value ?? 0);
  return new Intl.NumberFormat(language, { style: 'currency', currency: 'BRL' }).format(amount);
}

/**
 * Formats an ISO date (`2026-10-26`) in the interface language: `26/10/2026` or `10/26/2026`.
 *
 * @param iso - date as `yyyy-MM-dd`
 * @param language - language tag; defaults to the interface language
 * @returns the formatted date, or an empty string
 */
export function formatDate(iso: string | null | undefined, language: string = i18n.language): string {
  if (!iso) return '';
  const [year, month, day] = iso.split('-').map(Number);
  return new Intl.DateTimeFormat(language, { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(
    new Date(Date.UTC(year!, month! - 1, day!)),
  );
}

/**
 * Formats a billing period (`2026-11`) as month and year: `novembro de 2026` or `November 2026`.
 *
 * @param period - billing period as `yyyy-MM`
 * @param language - language tag; defaults to the interface language
 * @returns the formatted period
 */
export function formatPeriod(period: string, language: string = i18n.language): string {
  const [year, month] = period.split('-').map(Number);
  return new Intl.DateTimeFormat(language, { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(
    new Date(Date.UTC(year!, month! - 1, 1)),
  );
}

/**
 * Parses money typed by a person in the interface language: `1.234,56` (pt-BR) or `1,234.56` (en-US).
 *
 * @param text - typed text, optionally with `R$` and a minus sign
 * @param language - language tag; defaults to the interface language
 * @returns the amount, or `null` when the text is not a number
 */
export function parseMoney(text: string, language: string = i18n.language): number | null {
  const parts = new Intl.NumberFormat(language).formatToParts(1234.5);
  const group = parts.find((p) => p.type === 'group')?.value ?? '.';
  const decimal = parts.find((p) => p.type === 'decimal')?.value ?? ',';
  const cleaned = text.replace(/R\$|\s/g, '').split(group).join('').replace(decimal, '.');
  if (!/^-?\d+(\.\d{1,2})?$/.test(cleaned)) return null;
  return Number(cleaned);
}

/**
 * Converts an amount to whole cents, so that sums in the interface never use floating point (RNF-01).
 *
 * @param value - amount
 * @returns cents
 */
export function toCents(value: number | string | null | undefined): number {
  return Math.round(Number(value ?? 0) * 100);
}

/**
 * Converts cents back to an amount.
 *
 * @param cents - whole cents
 * @returns amount
 */
export function fromCents(cents: number): number {
  return cents / 100;
}

/**
 * Returns the billing period of a date.
 *
 * @param date - date; defaults to today
 * @returns period as `yyyy-MM`
 */
export function periodOf(date: Date = new Date()): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

/**
 * Returns a date as `yyyy-MM-dd` in the computer's time zone, unlike `toISOString`, which is UTC and gives the
 * next day in the Brazilian evening.
 *
 * @param date - date; defaults to today
 * @returns ISO date
 */
export function isoDateOf(date: Date = new Date()): string {
  return `${periodOf(date)}-${String(date.getDate()).padStart(2, '0')}`;
}

/**
 * Shifts a billing period by months.
 *
 * @param period - period as `yyyy-MM`
 * @param months - months to add, negative to go back
 * @returns shifted period
 */
export function shiftPeriod(period: string, months: number): string {
  const [year, month] = period.split('-').map(Number);
  const index = year! * 12 + (month! - 1) + months;
  return `${Math.floor(index / 12)}-${String((index % 12) + 1).padStart(2, '0')}`;
}

/**
 * Formats a percentage in the interface language, with one decimal place unless more are asked for.
 *
 * @param value - percentage such as `72.5`, or nothing
 * @param language - BCP 47 language; the interface language by default
 * @param maxDecimals - most decimal places shown, at least one
 * @returns text such as `72,5%`, or `—` when there is no value
 */
export function formatPercent(value: number | null | undefined, language: string = i18n.language, maxDecimals = 1): string {
  if (value === null || value === undefined) return '—';
  return `${new Intl.NumberFormat(language, { minimumFractionDigits: 1, maximumFractionDigits: Math.max(1, maxDecimals) }).format(value)}%`;
}

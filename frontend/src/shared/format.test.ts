import { describe, expect, it } from 'vitest';
import { formatDate, formatMoney, formatPeriod, fromCents, parseMoney, periodOf, shiftPeriod, toCents } from './format';

const nbsp = (s: string) => s.replace(/\u00a0/g, ' ');

describe('format', () => {
  it('I18N-05: money and dates follow the language, currency is always BRL', () => {
    expect(nbsp(formatMoney(1234.56, 'pt-BR'))).toBe('R$ 1.234,56');
    expect(formatMoney(1234.56, 'en-US')).toBe('R$1,234.56');
    expect(formatDate('2026-10-26', 'pt-BR')).toBe('26/10/2026');
    expect(formatDate('2026-10-26', 'en-US')).toBe('10/26/2026');
    expect(formatDate(undefined)).toBe('');
    expect(formatPeriod('2026-11', 'pt-BR')).toBe('novembro de 2026');
    expect(formatPeriod('2026-11', 'en-US')).toBe('November 2026');
  });

  it('I18N-05: typed money is parsed in the language of the interface', () => {
    expect(parseMoney('1.234,56', 'pt-BR')).toBe(1234.56);
    expect(parseMoney('R$ 10', 'pt-BR')).toBe(10);
    expect(parseMoney('1,234.56', 'en-US')).toBe(1234.56);
    expect(parseMoney('-5,00', 'pt-BR')).toBe(-5);
    expect(parseMoney('abc', 'pt-BR')).toBeNull();
    expect(parseMoney('1,234', 'pt-BR')).toBeNull();
  });

  it('RNF-01: quick sums use whole cents', () => {
    expect(toCents(0.1) + toCents(0.2)).toBe(30);
    expect(fromCents(toCents('48.02'))).toBe(48.02);
  });

  it('UI-02: billing periods shift across the year', () => {
    expect(shiftPeriod('2026-12', 1)).toBe('2027-01');
    expect(shiftPeriod('2027-01', -1)).toBe('2026-12');
    expect(periodOf(new Date(2026, 10, 3))).toBe('2026-11');
  });
});

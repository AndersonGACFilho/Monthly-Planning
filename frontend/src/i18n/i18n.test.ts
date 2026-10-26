import { describe, expect, it } from 'vitest';
import enUS from '../locales/en-US.json';
import ptBR from '../locales/pt-BR.json';
import { detectLanguage } from './i18n';

function keys(object: object, prefix = ''): string[] {
  return Object.entries(object).flatMap(([key, value]) =>
    value && typeof value === 'object' ? keys(value as object, `${prefix}${key}.`) : [`${prefix}${key}`],
  );
}

describe('detectLanguage', () => {
  it('I18N-03: picks the system language when supported', () => {
    expect(detectLanguage(['en-US', 'pt-BR'])).toBe('en-US');
    expect(detectLanguage(['pt-BR'])).toBe('pt-BR');
  });

  it('I18N-03: matches by language when the region differs', () => {
    expect(detectLanguage(['en-GB'])).toBe('en-US');
    expect(detectLanguage(['pt-PT'])).toBe('pt-BR');
  });

  it('I18N-03: falls back to pt-BR', () => {
    expect(detectLanguage(['de-DE', 'fr'])).toBe('pt-BR');
    expect(detectLanguage([])).toBe('pt-BR');
  });
});

describe('locales', () => {
  it('I18N-13: pt-BR and en-US have the same keys', () => {
    expect(keys(enUS).sort()).toEqual(keys(ptBR).sort());
  });
});

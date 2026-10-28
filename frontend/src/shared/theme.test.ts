import { afterEach, describe, expect, it } from 'vitest';
import { applyRememberedTheme, applyTheme, isDark } from './theme';

describe('theme', () => {
  afterEach(() => applyTheme(''));

  it('UI-08: a chosen theme is set on the page and remembered for the next start', () => {
    applyTheme('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(isDark()).toBe(true);
    delete document.documentElement.dataset.theme;
    applyRememberedTheme();
    expect(document.documentElement.dataset.theme).toBe('dark');
    applyTheme('light');
    expect(isDark()).toBe(false);
  });

  it('UI-08: empty or unknown values follow the system', () => {
    applyTheme('sepia');
    expect(document.documentElement.dataset.theme).toBeUndefined();
    expect(localStorage.getItem('theme')).toBeNull();
  });
});

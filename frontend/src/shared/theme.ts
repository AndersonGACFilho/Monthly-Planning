import { isTauri } from '@tauri-apps/api/core';

/** Colour theme choice: empty follows the system (UI-08). */
export type Theme = '' | 'light' | 'dark';

const STORAGE_KEY = 'theme';

/** Event fired on `window` whenever {@link applyTheme} runs. */
export const THEME_EVENT = 'themechange';

const normalize = (value: string | null | undefined): Theme => (value === 'light' || value === 'dark' ? value : '');

/**
 * Applies a theme to the page and the window, and remembers it so the next start paints in it before the
 * local service answers.
 *
 * @param value - saved setting, `light`, `dark` or empty for the system
 */
export function applyTheme(value: string | null | undefined): void {
  const theme = normalize(value);
  if (theme) document.documentElement.dataset.theme = theme;
  else delete document.documentElement.dataset.theme;
  try {
    if (theme) localStorage.setItem(STORAGE_KEY, theme);
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Storage blocked: the saved setting still applies once the service answers
  }
  window.dispatchEvent(new Event(THEME_EVENT));
  if (isTauri()) {
    void import('@tauri-apps/api/window')
      .then(({ getCurrentWindow }) => getCurrentWindow().setTheme(theme || null))
      .catch(() => undefined);
  }
}

/**
 * Applies the theme remembered from the last run, if any.
 */
export function applyRememberedTheme(): void {
  let remembered: string | null = null;
  try {
    remembered = localStorage.getItem(STORAGE_KEY);
  } catch {
    // Storage blocked: start with the system theme
  }
  applyTheme(remembered);
}

/**
 * Tells whether the page is dark right now, by choice or because the system is dark.
 *
 * @returns `true` when dark colours are shown
 */
export function isDark(): boolean {
  const theme = normalize(document.documentElement.dataset.theme);
  if (theme) return theme === 'dark';
  return typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

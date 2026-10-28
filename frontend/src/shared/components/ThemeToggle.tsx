import { useQueryClient } from '@tanstack/react-query';
import { Moon, Sun } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changeSettings, getSettingsQueryKey } from '../../api/generated/endpoints/settings-controller/settings-controller';
import { applyTheme, isDark, THEME_EVENT } from '../theme';

/**
 * Header button that switches between light and dark colours at once and saves the choice (UI-08). Settings
 * also offer "same as the system".
 *
 * @returns the button
 */
export function ThemeToggle() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [dark, setDark] = useState(isDark);
  useEffect(() => {
    const update = () => setDark(isDark());
    const media = typeof window.matchMedia === 'function' ? window.matchMedia('(prefers-color-scheme: dark)') : null;
    window.addEventListener(THEME_EVENT, update);
    media?.addEventListener('change', update);
    return () => {
      window.removeEventListener(THEME_EVENT, update);
      media?.removeEventListener('change', update);
    };
  }, []);
  const label = dark ? t('theme.toLight') : t('theme.toDark');
  return (
    <button
      type="button"
      className="icon-button"
      title={label}
      aria-label={label}
      onClick={async () => {
        const next = dark ? 'light' : 'dark';
        applyTheme(next);
        await changeSettings({ theme: next }).catch(() => undefined);
        await queryClient.invalidateQueries({ queryKey: getSettingsQueryKey() });
      }}
    >
      {dark ? <Sun size={18} aria-hidden /> : <Moon size={18} aria-hidden />}
    </button>
  );
}

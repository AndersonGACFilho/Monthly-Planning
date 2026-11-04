import { isTauri } from '@tauri-apps/api/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { reportError } from '../../app/queryClient';
import { addShortcut, removeShortcut, shortcutStatus, type ShortcutStatus } from './menuShortcut';

/**
 * Settings section that adds the app to the system menu of this computer, so searching finds it (POR-18). Never
 * done by itself: the entry is the one thing the app writes outside its folder, so only these buttons create,
 * fix or remove it. Only in the desktop app, and only where the system has a menu the app can use.
 *
 * @returns the section, or nothing in the browser preview or without a system menu
 */
export function MenuShortcutSettings() {
  return isTauri() ? <MenuShortcutSection /> : null;
}

/** Reads the entry; a system without a menu the app can use hides the section. */
async function loadStatus(set: (status: ShortcutStatus | null) => void): Promise<void> {
  set(await shortcutStatus().catch(() => null));
}

function MenuShortcutSection() {
  const { t } = useTranslation();
  const [status, setStatus] = useState<ShortcutStatus | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void loadStatus(setStatus);
  }, []);

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    try {
      await action();
    } catch (error) {
      reportError(error);
    }
    await loadStatus(setStatus);
    setBusy(false);
  };

  if (!status) return null;
  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="menu-shortcut-title">
      <h2 id="menu-shortcut-title" className="text-lg font-semibold">
        {t('menuShortcut.title')}
      </h2>
      <p className="text-sm" aria-live="polite">
        {status.state === 'elsewhere' ? t('menuShortcut.elsewhere', { folder: status.folder }) : t(`menuShortcut.${status.state}`)}
      </p>
      <div className="flex flex-wrap gap-2">
        {status.state !== 'current' && (
          <button type="button" className="button button-primary" disabled={busy} onClick={() => void run(addShortcut)}>
            {t(status.state === 'absent' ? 'menuShortcut.add' : 'menuShortcut.fix')}
          </button>
        )}
        {status.state !== 'absent' && (
          <button type="button" className="button" disabled={busy} onClick={() => void run(removeShortcut)}>
            {t('menuShortcut.remove')}
          </button>
        )}
      </div>
      <p className="text-xs text-(--color-text-muted)">{t('menuShortcut.howItWorks')}</p>
    </section>
  );
}

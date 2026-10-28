import { isTauri } from '@tauri-apps/api/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useUpdateStore } from './updateStore';
import { installedVersion } from './updates';

/**
 * Settings section with the installed version and a manual check for updates (POR-16). Only in the desktop app,
 * which is the only one that can replace its own files.
 *
 * @returns the section, or nothing in the browser preview
 */
export function UpdateSettings() {
  return isTauri() ? <UpdateSection /> : null;
}

function UpdateSection() {
  const { t } = useTranslation();
  const { phase, info, check, download } = useUpdateStore();
  const [version, setVersion] = useState<string | null>(null);

  useEffect(() => {
    void installedVersion().then(setVersion);
  }, []);

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="update-title">
      <h2 id="update-title" className="text-lg font-semibold">
        {t('update.title')}
      </h2>
      {version && <p className="text-sm">{t('update.installed', { version })}</p>}
      <div className="flex flex-wrap items-center gap-2">
        <button type="button" className="button" disabled={phase === 'checking' || phase === 'downloading'} onClick={() => void check()}>
          {t('update.check')}
        </button>
        {phase === 'available' && info && (
          <button type="button" className="button button-primary" onClick={() => void download()}>
            {t('update.installVersion', { version: info.version })}
          </button>
        )}
      </div>
      <p className="text-sm" aria-live="polite">
        {phase === 'checking' && t('update.checking')}
        {phase === 'upToDate' && t('update.upToDate')}
        {phase === 'checkFailed' && t('update.checkFailed')}
        {phase === 'downloading' && t('update.seeNotice')}
        {phase === 'ready' && info && t('update.ready', { version: info.version })}
      </p>
      <p className="text-xs text-(--color-text-muted)">{t('update.howItWorks')}</p>
    </section>
  );
}

import { isTauri } from '@tauri-apps/api/core';
import { useTranslation } from 'react-i18next';
import { backupNow, useBackups } from '../../api/generated/endpoints/backup-controller/backup-controller';
import { useDatabase } from '../../api/generated/endpoints/database-controller/database-controller';
import { apiFetchFile } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { useDatabaseExport } from '../database/useDatabaseExport';
import { backupLabel, parseBackupName } from '../../shared/backupName';
import { useFileExport } from '../../shared/hooks/useFileExport';
import { useDatabaseSwitch } from '../../shared/hooks/useDatabaseSwitch';
import { useSaved } from '../../shared/hooks/useSaved';
import { useNotifications } from '../../shared/state/notifications';

/**
 * Backups and export of all data: automatic copies, "back up now" to data/backups or to a chosen file, JSON or CSV
 * export, and the advice to close the app before removing the drive (POR-12, POR-13, RNF-07). Each backup can be
 * opened to check it, with a way back to the live data, or restored as the live data (IMP-58).
 *
 * @returns the section
 */
export function BackupSettings() {
  const { t, i18n } = useTranslation();
  const saved = useSaved();
  const { data: database } = useDatabase();
  const { openBackup, openLive, restore } = useDatabaseSwitch();
  const { busy: exporting, run } = useFileExport();
  const databaseExport = useDatabaseExport();
  const live = database?.live !== false;
  const push = useNotifications((state) => state.push);
  const { data: files } = useBackups();

  const backup = async (choose: boolean) => {
    try {
      let destination: string | undefined;
      if (choose) {
        const { save } = await import('@tauri-apps/plugin-dialog');
        destination = (await save({ defaultPath: `planejamento-mensal-${new Date().toISOString().slice(0, 10)}.db` })) ?? undefined;
        if (!destination) return;
      }
      const result = await backupNow({ destination });
      await saved();
      push({ kind: 'success', title: t('backup.done', { file: result.file }) });
    } catch (error) {
      reportError(error);
    }
  };

  const exportAll = (format: 'json' | 'csv') =>
    void run(() => apiFetchFile(`/api/data-export?format=${format}`), `planejamento-mensal.${format === 'csv' ? 'zip' : 'json'}`);

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="backup-title">
      <h2 id="backup-title" className="text-lg font-semibold">
        {t('backup.title')}
      </h2>
      <p className="text-sm">{t('backup.automatic')}</p>
      <p className="text-sm font-medium text-amber-800">{t('backup.eject')}</p>
      <div className="flex flex-wrap gap-2">
        <button type="button" className="button button-primary" disabled={!live} onClick={() => void backup(false)}>
          {t('backup.now')}
        </button>
        {isTauri() && (
          <button type="button" className="button" disabled={!live} onClick={() => void backup(true)}>
            {t('backup.nowTo')}
          </button>
        )}
        <button type="button" className="button" disabled={exporting} onClick={() => exportAll('json')}>
          {t('backup.exportJson')}
        </button>
        <button type="button" className="button" disabled={exporting} onClick={() => exportAll('csv')}>
          {t('backup.exportCsv')}
        </button>
        <button
          type="button"
          className="button"
          disabled={databaseExport.busy || !databaseExport.live}
          onClick={() => void databaseExport.exportDatabase()}
        >
          {t('backup.exportDb')}
        </button>
      </div>
      {!live && (
        <p className="flex flex-wrap items-center gap-2 text-sm">
          {t('database.viewing', { backup: backupLabel(database?.backup ?? '', i18n.language) })}
          <button type="button" className="button button-primary" onClick={() => void openLive()}>
            {t('database.goLive')}
          </button>
        </p>
      )}
      {files && files.length > 0 && (
        <section className="flex flex-col gap-2" aria-labelledby="backup-list-title">
          <h3 id="backup-list-title" className="font-semibold">
            {t('backup.list')}
          </h3>
          <p className="text-xs text-(--color-text-muted)">{t('database.hint')}</p>
          <div className="max-h-80 overflow-auto">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">{t('database.when')}</th>
                  <th scope="col">{t('database.reason')}</th>
                  <th scope="col">
                    <span className="sr-only">{t('common.actions')}</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {files.map((name) => {
                  const label = backupLabel(name, i18n.language);
                  const open = database?.backup === name;
                  return (
                    <tr key={name} className={open ? 'bg-amber-50 dark:bg-amber-950/40' : undefined}>
                      <th scope="row" className="font-normal" title={name}>
                        {label}
                        {open && <span className="badge badge-warning ml-2">{t('database.openNow')}</span>}
                      </th>
                      <td>{t(`database.reasons.${parseBackupName(name).reason}`)}</td>
                      <td className="text-right whitespace-nowrap">
                        {!open && (
                          <button
                            type="button"
                            className="button"
                            aria-label={t('database.openFor', { backup: label })}
                            onClick={() => void openBackup(name)}
                          >
                            {t('database.open')}
                          </button>
                        )}
                        <button
                          type="button"
                          className="button ml-2"
                          aria-label={t('database.restoreFor', { backup: label })}
                          onClick={() => void restore(name)}
                        >
                          {t('database.restore')}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </section>
      )}
      <p className="text-xs text-(--color-text-muted)">
        <strong>{t('shortcuts.title')}:</strong> {t('shortcuts.list')}
      </p>
    </section>
  );
}

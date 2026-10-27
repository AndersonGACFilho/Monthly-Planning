import { isTauri } from '@tauri-apps/api/core';
import { useTranslation } from 'react-i18next';
import { backupNow, useBackups } from '../../api/generated/endpoints/backup-controller/backup-controller';
import { apiFetchFile } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { downloadFile } from '../../shared/desktop';
import { useSaved } from '../../shared/hooks/useSaved';
import { useNotifications } from '../../shared/state/notifications';

/**
 * Backups and export of all data: automatic copies, "back up now" to data/backups or to a chosen file, JSON or CSV
 * export, and the advice to close the app before removing the drive (POR-12, POR-13, RNF-07).
 *
 * @returns the section
 */
export function BackupSettings() {
  const { t } = useTranslation();
  const saved = useSaved();
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

  const exportAll = async (format: 'json' | 'csv') => {
    try {
      const file = await apiFetchFile(`/api/data-export?format=${format}`);
      downloadFile(file.blob, file.fileName ?? `planejamento-mensal.${format === 'csv' ? 'zip' : 'json'}`);
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <section className="panel flex flex-col gap-3" aria-labelledby="backup-title">
      <h2 id="backup-title" className="text-lg font-semibold">
        {t('backup.title')}
      </h2>
      <p className="text-sm">{t('backup.automatic')}</p>
      <p className="text-sm font-medium text-amber-800">{t('backup.eject')}</p>
      <div className="flex flex-wrap gap-2">
        <button type="button" className="button button-primary" onClick={() => void backup(false)}>
          {t('backup.now')}
        </button>
        {isTauri() && (
          <button type="button" className="button" onClick={() => void backup(true)}>
            {t('backup.nowTo')}
          </button>
        )}
        <button type="button" className="button" onClick={() => void exportAll('json')}>
          {t('backup.exportJson')}
        </button>
        <button type="button" className="button" onClick={() => void exportAll('csv')}>
          {t('backup.exportCsv')}
        </button>
      </div>
      {files && files.length > 0 && (
        <details>
          <summary className="cursor-pointer text-sm">{t('backup.list')}</summary>
          <ul className="mt-1 text-xs text-(--color-text-muted)">
            {files.map((name) => (
              <li key={name}>{name}</li>
            ))}
          </ul>
        </details>
      )}
      <p className="text-xs text-(--color-text-muted)">
        <strong>{t('shortcuts.title')}:</strong> {t('shortcuts.list')}
      </p>
    </section>
  );
}

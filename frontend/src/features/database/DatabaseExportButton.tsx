import { DatabaseBackup } from 'lucide-react';
import { useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useUiStore } from '../../shared/state/ui';
import { useDatabaseExport } from './useDatabaseExport';

/**
 * Top bar button that exports the whole database as one file (IMP-69); Ctrl+Shift+E does the same (UI-01).
 *
 * @returns the button
 */
export function DatabaseExportButton() {
  const { t } = useTranslation();
  const { live, busy, exportDatabase } = useDatabaseExport();
  const requests = useUiStore((state) => state.databaseExportRequests);
  const handled = useRef(requests);

  useEffect(() => {
    if (requests === handled.current) return;
    handled.current = requests;
    if (live) void exportDatabase();
  }, [requests, live, exportDatabase]);

  return (
    <button
      type="button"
      className="button"
      disabled={busy || !live}
      title={live ? t('database.exportHint') : t('database.exportBlocked')}
      onClick={() => void exportDatabase()}
    >
      <DatabaseBackup size={16} aria-hidden /> {t('database.export')}
    </button>
  );
}

import { useDatabase } from '../../api/generated/endpoints/database-controller/database-controller';
import { apiFetchFile } from '../../api/http';
import { useFileExport } from '../../shared/hooks/useFileExport';

/**
 * Exports the whole database as one SQLite file (IMP-69), with the feedback of every file export (IMP-62). Like a
 * backup, it is not offered while a backup is open.
 *
 * @returns whether the live data is open, whether an export is running, and the action
 */
export function useDatabaseExport() {
  const { data: database } = useDatabase();
  const { busy, run } = useFileExport();
  const live = database?.live !== false;
  const exportDatabase = () =>
    run(() => apiFetchFile('/api/data-export?format=db'), `planejamento-mensal-${new Date().toISOString().slice(0, 10)}.db`);
  return { live, busy, exportDatabase };
}

import { DatabaseBackup } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useDatabase } from '../api/generated/endpoints/database-controller/database-controller';
import { backupLabel } from '../shared/backupName';
import { useDatabaseSwitch } from '../shared/hooks/useDatabaseSwitch';

/**
 * Band under the header while a backup is open (IMP-58): says which one, that changes are dropped when the live data
 * comes back, and offers to go back or to restore it.
 *
 * @returns the band, or nothing while the app works on the live data
 */
export function DatabaseBanner() {
  const { t, i18n } = useTranslation();
  const { data } = useDatabase();
  const { openLive, restore } = useDatabaseSwitch();
  if (!data || data.live || !data.backup) return null;
  const backup = data.backup;
  return (
    <div
      role="status"
      className="flex flex-wrap items-center gap-3 border-b border-amber-500 bg-amber-100 px-4 py-2 text-sm text-amber-950 dark:border-amber-600 dark:bg-amber-950 dark:text-amber-100"
    >
      <DatabaseBackup size={18} aria-hidden />
      <span className="flex-1">
        <strong>{t('database.viewing', { backup: backupLabel(backup, i18n.language) })}</strong> {t('database.viewingHint')}
      </span>
      <button type="button" className="button" onClick={() => void restore(backup)}>
        {t('database.restore')}
      </button>
      <button type="button" className="button button-primary" onClick={() => void openLive()}>
        {t('database.goLive')}
      </button>
    </div>
  );
}

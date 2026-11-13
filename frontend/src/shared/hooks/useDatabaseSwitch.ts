import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { importDatabase, restoreBackup, switchDatabase } from '../../api/generated/endpoints/database-controller/database-controller';
import { reportError } from '../../app/queryClient';
import { backupLabel } from '../backupName';
import { useNotifications } from '../state/notifications';

/** Actions that change the database the app works on (IMP-58). */
export interface DatabaseSwitch {
  /** Opens a copy of a backup to check it. */
  openBackup: (fileName: string) => Promise<void>;
  /** Goes back to the live data, dropping the copy. */
  openLive: () => Promise<void>;
  /** Asks for confirmation, then makes a backup the live data. */
  restore: (fileName: string) => Promise<void>;
  /** Imports a database file and opens it to check (IMP-72). */
  importFile: (file: File) => Promise<void>;
}

/**
 * Returns the actions that switch between the live data, the backups and an imported database file. After each one
 * every cached query is refreshed, so every screen shows the database now in use.
 *
 * @returns the actions
 */
export function useDatabaseSwitch(): DatabaseSwitch {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const push = useNotifications((state) => state.push);

  const run = useCallback(
    async (action: () => Promise<unknown>, title: string) => {
      try {
        await action();
        await queryClient.invalidateQueries();
        push({ kind: 'success', title });
      } catch (error) {
        reportError(error);
      }
    },
    [queryClient, push],
  );

  return useMemo(
    () => ({
      openBackup: (fileName) =>
        run(() => switchDatabase({ backup: fileName }), t('database.opened', { backup: backupLabel(fileName, i18n.language) })),
      openLive: () => run(() => switchDatabase({}), t('database.backToLive')),
      restore: async (fileName) => {
        const backup = backupLabel(fileName, i18n.language);
        if (!window.confirm(t('database.confirmRestore', { backup }))) return;
        await run(() => restoreBackup({ backup: fileName }), t('database.restored', { backup }));
      },
      importFile: (file) => run(() => importDatabase({ file }), t('database.imported')),
    }),
    [run, t, i18n.language],
  );
}

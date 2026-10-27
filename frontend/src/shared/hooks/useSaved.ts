import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNotifications } from '../state/notifications';

/**
 * Returns a callback for successful saves: refreshes cached data and shows a short confirmation.
 *
 * @returns function to call after a successful mutation
 */
export function useSaved() {
  const queryClient = useQueryClient();
  const push = useNotifications((state) => state.push);
  const { t } = useTranslation();
  return useCallback(async () => {
    await queryClient.invalidateQueries();
    push({ kind: 'success', title: t('common.saved') });
  }, [queryClient, push, t]);
}

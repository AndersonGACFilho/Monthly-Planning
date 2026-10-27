import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { ProblemError } from '../api/http';
import i18n from '../i18n/i18n';
import { useNotifications } from '../shared/state/notifications';

function notify(error: unknown): void {
  const push = useNotifications.getState().push;
  if (error instanceof ProblemError) {
    const fieldErrors = error.problem.errors?.map((e) => `${e.field}: ${e.message}`).join('; ');
    push({ kind: 'error', title: error.problem.title ?? i18n.t('error.unexpected'), detail: fieldErrors || error.problem.detail });
  } else {
    push({ kind: 'error', title: i18n.t('error.network') });
  }
}

/**
 * Creates the server-state cache with the single error interceptor of the interface: every failed call
 * shows the localized problem sent by the backend (IMP-15, API-03).
 *
 * @returns the query client
 */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    queryCache: new QueryCache({ onError: notify }),
    mutationCache: new MutationCache({ onError: notify }),
    defaultOptions: { queries: { staleTime: 30_000, retry: 1 } },
  });
}

/**
 * Shows an error from a direct API call (outside a mutation hook) through the same interceptor.
 *
 * @param error - thrown error
 */
export function reportError(error: unknown): void {
  notify(error);
}

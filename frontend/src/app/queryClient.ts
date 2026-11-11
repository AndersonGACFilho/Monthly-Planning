import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { reportClientError } from '../api/generated/endpoints/diagnostics-controller/diagnostics-controller';
import { ProblemError } from '../api/http';
import { useDebugMode } from '../shared/state/debugMode';
import { useNotifications } from '../shared/state/notifications';
import { describeError, describeThrown } from './errorNotice';

function notify(error: unknown): void {
  console.error(error);
  const debug = useDebugMode.getState().enabled;
  useNotifications.getState().push(describeError(error, debug));
  if (debug && !(error instanceof ProblemError)) {
    sendToLog(error);
  }
}

/** Writes an interface error to the backend log; the backend logs its own problems already (RNF-11). */
function sendToLog(error: unknown): void {
  const [message, ...stack] = describeThrown(error).split('\n');
  reportClientError({ message: message ?? String(error), detail: stack.join('\n') || undefined, url: window.location.pathname })
    .catch(() => undefined);
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
 * Shows an error from a direct API call (outside a mutation hook), a render error or an unhandled one through the
 * same interceptor; in debug mode interface errors also reach the backend log (RNF-11).
 *
 * @param error - thrown error
 */
export function reportError(error: unknown): void {
  notify(error);
}

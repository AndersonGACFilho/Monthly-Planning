import { useEffect } from 'react';
import { useDiagnostics } from '../api/generated/endpoints/diagnostics-controller/diagnostics-controller';
import { useDebugMode } from '../shared/state/debugMode';

/**
 * Loads the debug mode of the backend into the interface when the app opens, so error notices know whether to show
 * their details (RNF-11).
 */
export function useDebugModeSync(): void {
  const { data } = useDiagnostics();
  const set = useDebugMode((state) => state.set);
  useEffect(() => {
    if (data) set(Boolean(data.debugMode));
  }, [data, set]);
}

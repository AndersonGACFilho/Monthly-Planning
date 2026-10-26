import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { getBackendConfig } from '../api/backendConfig';

/** Props of {@link BackendGate}. */
export interface BackendGateProps {
  /** Content shown once the local service answers. */
  children: ReactNode;
}

type GateState = 'loading' | 'ready' | 'failed';

/**
 * Shows a loading screen until the shell reports that the local service is healthy (POR-08), then renders
 * the app. If the service cannot start, shows a short error instead.
 *
 * @param props - {@link BackendGateProps}
 * @returns the loading screen, the error or the children
 */
export function BackendGate({ children }: BackendGateProps) {
  const { t } = useTranslation();
  const [state, setState] = useState<GateState>('loading');

  useEffect(() => {
    let active = true;
    getBackendConfig().then(
      () => active && setState('ready'),
      () => active && setState('failed'),
    );
    return () => {
      active = false;
    };
  }, []);

  if (state === 'ready') return children;
  return (
    <div role={state === 'failed' ? 'alert' : 'status'} className="flex h-full items-center justify-center">
      <p className="text-(--color-text-muted)">{t(state === 'failed' ? 'error.network' : 'app.loading')}</p>
    </div>
  );
}

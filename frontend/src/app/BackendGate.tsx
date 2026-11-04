import { AlertTriangle, CalendarRange } from 'lucide-react';
import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { getBackendConfig, resetBackendConfig } from '../api/backendConfig';

/** Props of {@link BackendGate}. */
export interface BackendGateProps {
  /** Content shown once the local service answers. */
  children: ReactNode;
}

type GateState = 'loading' | 'ready' | 'failed';

/** Seconds after which each loading step is shown: what the service is doing at that point of a normal start. */
export const LOADING_STEPS = [
  { after: 0, key: 'starting' },
  { after: 2, key: 'opening' },
  { after: 6, key: 'checking' },
  { after: 15, key: 'slow' },
] as const;

/**
 * Returns the loading step for the time waited so far.
 *
 * @param seconds - seconds since the loading screen appeared
 * @returns key of the step under `loading.steps`
 */
export function loadingStep(seconds: number): (typeof LOADING_STEPS)[number]['key'] {
  return LOADING_STEPS.filter((step) => seconds >= step.after).at(-1)!.key;
}

/**
 * Shows a loading screen until the shell reports that the local service is healthy (POR-08, IMP-57): the app name,
 * a progress bar and what the service is doing, from starting through the backup and migration of the data, with a
 * note when it takes longer than usual. If the service cannot start, shows what to check and a retry button.
 *
 * @param props - {@link BackendGateProps}
 * @returns the loading screen, the error or the children
 */
export function BackendGate({ children }: BackendGateProps) {
  const { t } = useTranslation();
  const [state, setState] = useState<GateState>('loading');
  const [attempt, setAttempt] = useState(0);
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    let active = true;
    getBackendConfig().then(
      () => active && setState('ready'),
      () => active && setState('failed'),
    );
    const timer = setInterval(() => setSeconds((s) => s + 1), 1000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [attempt]);

  const retry = () => {
    resetBackendConfig();
    setSeconds(0);
    setState('loading');
    setAttempt((n) => n + 1);
  };

  if (state === 'ready') return children;
  const failed = state === 'failed';
  return (
    <div className="flex h-full items-center justify-center bg-(--color-surface-muted) p-4">
      <div
        role={failed ? 'alert' : 'status'}
        aria-live="polite"
        className="flex w-full max-w-sm flex-col items-center gap-4 rounded-lg border border-(--color-border) bg-(--color-surface) p-8 text-center shadow-sm"
      >
        <span className={`rounded-full p-3 ${failed ? 'bg-red-100 text-red-700' : 'bg-(--color-surface-muted) text-(--color-accent)'}`}>
          {failed ? <AlertTriangle size={32} aria-hidden /> : <CalendarRange size={32} aria-hidden />}
        </span>
        <h1 className="text-xl font-semibold">{t('app.name')}</h1>
        {failed ? (
          <>
            <p className="font-medium">{t('error.network')}</p>
            <p className="text-sm text-(--color-text-muted)">{t('loading.failedHint')}</p>
            <button type="button" className="button button-primary" onClick={retry}>
              {t('loading.retry')}
            </button>
          </>
        ) : (
          <>
            <div className="h-1.5 w-full overflow-hidden rounded-full bg-(--color-surface-muted)" aria-hidden>
              <div className="loading-bar h-full w-1/3 rounded-full bg-(--color-accent)" />
            </div>
            <p className="text-sm">{t(`loading.steps.${loadingStep(seconds)}`)}</p>
            <p className="sr-only">{t('app.loading')}</p>
          </>
        )}
      </div>
    </div>
  );
}

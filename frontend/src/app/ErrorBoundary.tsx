import { Component, type ErrorInfo, type ReactNode, useEffect } from 'react';
import { Translation } from 'react-i18next';
import { useRouteError } from 'react-router';
import { reportError } from './queryClient';

interface ErrorBoundaryState {
  failed: boolean;
}

/**
 * Catches an error while a screen renders, reports it like any other error (RNF-11) and offers to reload instead of
 * leaving a blank window.
 */
export class ErrorBoundary extends Component<{ children: ReactNode }, ErrorBoundaryState> {
  override state: ErrorBoundaryState = { failed: false };

  /**
   * Switches to the fallback after a render error.
   *
   * @returns the new state
   */
  static getDerivedStateFromError(): ErrorBoundaryState {
    return { failed: true };
  }

  override componentDidCatch(error: Error, info: ErrorInfo): void {
    error.stack = `${error.stack ?? ''}${info.componentStack ?? ''}`;
    reportError(error);
  }

  override render(): ReactNode {
    return this.state.failed ? <ErrorFallback /> : this.props.children;
  }
}

/**
 * Error raised by a screen of the router: reported once and shown inside the layout, so the menu stays (RNF-11).
 *
 * @returns the fallback
 */
export function RouteError() {
  const error = useRouteError();
  useEffect(() => reportError(error), [error]);
  return <ErrorFallback />;
}

function ErrorFallback() {
  return (
    <Translation>
      {(t) => (
        <div role="alert" className="panel m-4 flex flex-col items-start gap-2">
          <p>{t('error.screenFailed')}</p>
          <button type="button" className="button" onClick={() => window.location.reload()}>
            {t('error.reload')}
          </button>
        </div>
      )}
    </Translation>
  );
}

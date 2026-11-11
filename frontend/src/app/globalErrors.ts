import { reportError } from './queryClient';

/**
 * Reports errors that escape every handler, such as a failed promise nobody awaited (RNF-11).
 *
 * @param target - where the errors are raised; the window in the app
 */
export function installGlobalErrorHandlers(target: EventTarget = window): void {
  target.addEventListener('error', (event) => reportError((event as ErrorEvent).error ?? (event as ErrorEvent).message));
  target.addEventListener('unhandledrejection', (event) => reportError((event as PromiseRejectionEvent).reason));
}

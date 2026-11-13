import { ProblemError } from '../api/http';
import i18n from '../i18n/i18n';
import type { Notice } from '../shared/state/notifications';

const STACK_LINES = 6;

/**
 * Turns an error into the notice the user sees (IMP-15). In debug mode the notice also carries the technical details
 * to copy and stays on screen until closed (RNF-11).
 *
 * @param error - thrown error
 * @param debug - whether the debug mode is on
 * @returns the notice to show
 */
export function describeError(error: unknown, debug: boolean): Omit<Notice, 'id'> {
  const notice = summaryOf(error);
  return debug ? { ...notice, details: detailsOf(error), sticky: true } : notice;
}

function summaryOf(error: unknown): Omit<Notice, 'id'> {
  if (error instanceof ProblemError) {
    const fieldErrors = error.problem.errors?.map((e) => `${e.field}: ${e.message}`).join('; ');
    return { kind: 'error', title: error.problem.title ?? i18n.t('error.unexpected'), detail: fieldErrors || error.problem.detail };
  }
  if (error instanceof TypeError) {
    return { kind: 'error', title: i18n.t('error.network') };
  }
  return { kind: 'error', title: i18n.t('error.unexpected') };
}

function detailsOf(error: unknown): string {
  if (error instanceof ProblemError) {
    const { problem, request } = error;
    return [
      problem.code && `code: ${problem.code}`,
      `status: ${problem.status}`,
      `request: ${request.requestId}`,
      `${request.method} ${request.url}`,
    ]
      .filter(Boolean)
      .join('\n');
  }
  return describeThrown(error);
}

/**
 * Describes any thrown value in a few lines: name, message and the top of the stack.
 *
 * @param error - thrown value
 * @returns text for the log or the clipboard
 */
export function describeThrown(error: unknown): string {
  if (error instanceof Error) {
    const stack = error.stack?.split('\n').slice(1, STACK_LINES).join('\n');
    return [`${error.name}: ${error.message}`, stack].filter(Boolean).join('\n');
  }
  return String(error);
}

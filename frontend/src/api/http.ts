import i18n from '../i18n/i18n';
import { getBackendConfig } from './backendConfig';

/** RFC 9457 problem returned by the API for every error (API-03). */
export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  errors?: { field: string; message: string }[];
}

/** Error thrown when the API answers with a problem; carries the localized texts from the backend. */
export class ProblemError extends Error {
  /**
   * @param problem - problem body sent by the API
   */
  constructor(readonly problem: Problem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
    this.name = 'ProblemError';
  }
}

/**
 * Calls the local API: adds the session token, the interface language and the HAL media type, and turns
 * error responses into {@link ProblemError}. Used by every generated hook (Orval mutator, API-12).
 *
 * @param url - path such as `/api/members`, or an absolute link taken from `_links`
 * @param init - fetch options
 * @returns the parsed JSON body, or `undefined` for empty responses
 * @throws ProblemError when the API answers with a status of 400 or more
 */
export async function apiFetch<T>(url: string, init: RequestInit = {}): Promise<T> {
  const { baseUrl, token } = await getBackendConfig();
  const headers = new Headers(init.headers);
  headers.set('X-Session-Token', token);
  headers.set('Accept-Language', i18n.language);
  if (!headers.has('Accept')) headers.set('Accept', 'application/hal+json, application/problem+json');
  const target = /^https?:\/\//.test(url) ? url : `${baseUrl}${url}`;
  const response = await fetch(target, { ...init, headers });
  const text = await response.text();
  const body: unknown = text ? JSON.parse(text) : undefined;
  if (!response.ok) {
    throw new ProblemError((body as Problem | undefined) ?? { status: response.status });
  }
  return body as T;
}

export default apiFetch;

import i18n from '../i18n/i18n';
import { getBackendConfig } from './backendConfig';

/** RFC 9457 problem returned by the API for every error (API-03). */
export interface Problem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  code?: string;
  /** Id of the request in the backend log (RNF-11). */
  requestId?: string;
  errors?: { field: string; message: string }[];
}

/** The call a problem answered, to trace it in the backend log (RNF-11). */
export interface FailedRequest {
  method: string;
  url: string;
  requestId: string;
}

/** Header that carries the id of a request in both directions (RNF-11). */
export const REQUEST_ID_HEADER = 'X-Request-Id';

/** Error thrown when the API answers with a problem; carries the localized texts from the backend. */
export class ProblemError extends Error {
  /**
   * @param problem - problem body sent by the API
   * @param request - the call that failed
   */
  constructor(
    readonly problem: Problem,
    readonly request: FailedRequest,
  ) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
    this.name = 'ProblemError';
  }
}

function newRequestId(): string {
  return `ui-${Math.random().toString(36).slice(2, 12)}`;
}

function parseJson(text: string): unknown {
  try {
    return text ? JSON.parse(text) : undefined;
  } catch {
    return undefined;
  }
}

function problemOf(response: Response, text: string, method: string, url: string, sentId: string): ProblemError {
  const body = parseJson(text) as Problem | undefined;
  const problem = body?.status ? body : { status: response.status };
  const requestId = problem.requestId ?? response.headers.get(REQUEST_ID_HEADER) ?? sentId;
  return new ProblemError(problem, { method, url, requestId });
}

/**
 * Calls the local API: adds the session token, the interface language, the HAL media type and a request id
 * (RNF-11), and turns error responses into {@link ProblemError}. Used by every generated hook (Orval mutator, API-12).
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
  const requestId = newRequestId();
  headers.set(REQUEST_ID_HEADER, requestId);
  const target = /^https?:\/\//.test(url) ? url : `${baseUrl}${url}`;
  const response = await fetch(target, { ...init, headers });
  const text = await response.text();
  if (!response.ok) {
    throw problemOf(response, text, init.method ?? 'GET', url, requestId);
  }
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * Fetches a file from the local API, such as a statement PDF, with the session token.
 *
 * @param url - absolute URL or path starting at `/api`
 * @returns the file body
 * @throws ProblemError when the API answers with a status of 400 or more
 */
export async function apiFetchBlob(url: string): Promise<Blob> {
  return (await apiFetchFile(url)).blob;
}

/**
 * Fetches a file from the local API with the name the API suggests in `Content-Disposition`.
 *
 * @param url - absolute URL or path starting at `/api`
 * @returns the file body and its suggested name, or `null` when the API gives none
 * @throws ProblemError when the API answers with a status of 400 or more
 */
export async function apiFetchFile(url: string): Promise<{ blob: Blob; fileName: string | null }> {
  const { baseUrl, token } = await getBackendConfig();
  const target = /^https?:\/\//.test(url) ? url : `${baseUrl}${url}`;
  const requestId = newRequestId();
  const response = await fetch(target, {
    headers: { 'X-Session-Token': token, 'Accept-Language': i18n.language, [REQUEST_ID_HEADER]: requestId },
  });
  if (!response.ok) {
    throw problemOf(response, await response.text(), 'GET', url, requestId);
  }
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1];
  const fileName = encoded ? decodeURIComponent(encoded) : (/filename="?([^";]+)"?/.exec(disposition)?.[1] ?? null);
  return { blob: await response.blob(), fileName };
}

export default apiFetch;

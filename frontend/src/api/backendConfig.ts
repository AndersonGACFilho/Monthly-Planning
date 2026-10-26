import { invoke, isTauri } from '@tauri-apps/api/core';

/** Where the local API runs and the token that opens it (POR-08, IMP-04). */
export interface BackendConfig {
  /** Base URL such as `http://127.0.0.1:51234`, without trailing slash; empty for same-origin dev proxy. */
  baseUrl: string;
  /** Session token sent in the `X-Session-Token` header. */
  token: string;
}

let cached: Promise<BackendConfig> | undefined;

/**
 * Returns the backend address and session token.
 *
 * In the desktop app the Tauri shell owns them (command `backend_config`); in a plain browser during
 * development they come from `VITE_API_BASE_URL` and `VITE_SESSION_TOKEN`.
 *
 * @returns the configuration, resolved once and then cached
 */
export function getBackendConfig(): Promise<BackendConfig> {
  cached ??= isTauri()
    ? invoke<BackendConfig>('backend_config')
    : Promise.resolve({
        baseUrl: import.meta.env.VITE_API_BASE_URL ?? '',
        token: import.meta.env.VITE_SESSION_TOKEN ?? '',
      });
  return cached;
}

/**
 * Forgets the cached configuration. Only for tests.
 */
export function resetBackendConfig(): void {
  cached = undefined;
}

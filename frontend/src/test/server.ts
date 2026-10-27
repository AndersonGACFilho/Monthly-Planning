import { setupServer } from 'msw/node';
import { afterAll, afterEach, beforeAll, beforeEach, vi } from 'vitest';
import { resetBackendConfig } from '../api/backendConfig';

/** MSW server shared by screen tests; handlers are added per test with `server.use`. */
export const server = setupServer();

/**
 * Starts MSW and points the API client at a fake local backend for the current test file.
 */
export function useMockApi(): void {
  beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
  afterAll(() => server.close());
  afterEach(() => {
    server.resetHandlers();
    vi.unstubAllEnvs();
  });
  beforeEach(() => {
    resetBackendConfig();
    vi.stubEnv('VITE_API_BASE_URL', 'http://127.0.0.1:9999');
    vi.stubEnv('VITE_SESSION_TOKEN', 'test');
  });
}

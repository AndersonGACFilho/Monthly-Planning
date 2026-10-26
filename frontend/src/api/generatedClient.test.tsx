import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import type { ReactNode } from 'react';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { resetBackendConfig } from './backendConfig';
import { useRoot } from './generated/endpoints/root-controller/root-controller';
import { getRootControllerMock } from './generated/endpoints/root-controller/root-controller.msw';

const seenTokens: (string | null)[] = [];
const server = setupServer(
  http.get('*/api', ({ request }) => {
    seenTokens.push(request.headers.get('X-Session-Token'));
    return HttpResponse.json({ _links: { self: { href: 'http://127.0.0.1:9999/api' } } });
  }),
);

function wrapper({ children }: { children: ReactNode }) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('generated API client', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
  afterAll(() => server.close());
  afterEach(() => server.resetHandlers());
  beforeEach(() => {
    resetBackendConfig();
    vi.stubEnv('VITE_API_BASE_URL', 'http://127.0.0.1:9999');
    vi.stubEnv('VITE_SESSION_TOKEN', 'generated-test');
  });

  it('API-12: generated hook calls the API through apiFetch and returns the HAL body', async () => {
    const { result } = renderHook(() => useRoot(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?._links?.self?.href).toBe('http://127.0.0.1:9999/api');
    expect(seenTokens).toContain('generated-test');
  });

  it('API-12: generated MSW handlers serve mock data for screen tests', async () => {
    server.use(...getRootControllerMock());
    const { result } = renderHook(() => useRoot(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toBeDefined();
  });
});

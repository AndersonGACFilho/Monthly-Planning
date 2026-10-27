import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';
import { server, useMockApi } from '../../test/server';
import { useAction } from './useAction';

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>;
}

describe('useAction', () => {
  useMockApi();

  it('API-09: action is offered only when its link is present', () => {
    const { result } = renderHook(() => useAction({ _links: { self: { href: '/x' } } }, 'delete'), { wrapper });
    expect(result.current.available).toBe(false);
  });

  it('API-09, API-04: action calls the linked href with method, body and If-Match', async () => {
    let seen: { method: string; ifMatch: string | null; body: unknown } | undefined;
    server.use(
      http.patch('http://127.0.0.1:9999/api/members/1', async ({ request }) => {
        seen = { method: request.method, ifMatch: request.headers.get('If-Match'), body: await request.json() };
        return HttpResponse.json({ id: 1 });
      }),
    );
    const resource = { _links: { inactivate: { href: 'http://127.0.0.1:9999/api/members/1' } } };
    const { result } = renderHook(() => useAction(resource, 'inactivate'), { wrapper });
    expect(result.current.available).toBe(true);
    act(() => result.current.mutate({ method: 'PATCH', body: { active: false }, version: '"3"' }));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(seen).toEqual({ method: 'PATCH', ifMatch: '"3"', body: { active: false } });
  });
});

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n';
import { resetBackendConfig } from './backendConfig';
import { apiFetch, ProblemError } from './http';

describe('apiFetch', () => {
  beforeEach(() => {
    resetBackendConfig();
    vi.stubEnv('VITE_API_BASE_URL', 'http://127.0.0.1:9999');
    vi.stubEnv('VITE_SESSION_TOKEN', 'abc');
  });

  afterEach(() => {
    vi.unstubAllEnvs();
    vi.restoreAllMocks();
  });

  it('IMP-04: sends the session token, language and HAL media type', async () => {
    await i18n.changeLanguage('en-US');
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"ok":true}'));
    await expect(apiFetch('/api')).resolves.toEqual({ ok: true });
    const [url, init] = fetchMock.mock.calls[0]!;
    const headers = new Headers(init?.headers);
    expect(url).toBe('http://127.0.0.1:9999/api');
    expect(headers.get('X-Session-Token')).toBe('abc');
    expect(headers.get('Accept-Language')).toBe('en-US');
    expect(headers.get('Accept')).toContain('application/hal+json');
  });

  it('API-03: error responses become ProblemError with the backend texts', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response('{"status":404,"title":"Não encontrado","detail":"Não existe member com id 7."}', { status: 404 }),
    );
    const error = await apiFetch('/api/members/7').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ProblemError);
    expect((error as ProblemError).problem.status).toBe(404);
    expect((error as ProblemError).message).toBe('Não existe member com id 7.');
  });

  it('API-08: absolute links from _links are used as they are', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(''));
    await expect(apiFetch('http://127.0.0.1:9999/api/profile')).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[0]![0]).toBe('http://127.0.0.1:9999/api/profile');
  });
});

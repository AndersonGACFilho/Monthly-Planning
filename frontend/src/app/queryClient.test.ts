import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n';
import { useDebugMode } from '../shared/state/debugMode';
import { useNotifications } from '../shared/state/notifications';
import { server, useMockApi } from '../test/server';
import { reportError } from './queryClient';

const base = 'http://127.0.0.1:9999';

describe('reportError', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useNotifications.setState({ notices: [] });
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
  });
  afterEach(() => {
    useDebugMode.setState({ enabled: false });
    vi.restoreAllMocks();
  });

  it('RNF-11: an interface error goes to the console and, in debug mode, to the backend log', async () => {
    let sent: Record<string, unknown> | undefined;
    server.use(
      http.post(`${base}/api/diagnostics/client-errors`, async ({ request }) => {
        sent = (await request.json()) as Record<string, unknown>;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    useDebugMode.setState({ enabled: true });
    const error = new Error('x is undefined');
    reportError(error);
    expect(console.error).toHaveBeenCalledWith(error);
    expect(useNotifications.getState().notices).toHaveLength(1);
    await vi.waitFor(() => expect(sent).toMatchObject({ message: 'Error: x is undefined' }));
  });

  it('RNF-11: without debug mode nothing is sent to the backend', async () => {
    let sent = false;
    server.use(
      http.post(`${base}/api/diagnostics/client-errors`, () => {
        sent = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    reportError(new Error('quiet'));
    await new Promise((resolve) => setTimeout(resolve, 20));
    expect(sent).toBe(false);
  });
});

import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useDebugMode } from '../../shared/state/debugMode';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { DiagnosticsSettings } from './DiagnosticsSettings';

const base = 'http://127.0.0.1:9999';
const diagnostics = (debugMode: boolean) => ({ debugMode, logFolder: '/apps/pm/data/logs' });

describe('DiagnosticsSettings', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });
  afterEach(() => useDebugMode.setState({ enabled: false }));

  it('RNF-11: turns the debug mode on and shows where the log is', async () => {
    let body: unknown;
    server.use(
      http.get(`${base}/api/diagnostics`, () => HttpResponse.json(diagnostics(false))),
      http.put(`${base}/api/diagnostics`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(diagnostics(true));
      }),
    );
    renderWithProviders(<DiagnosticsSettings />);
    expect(await screen.findByText(/\/apps\/pm\/data\/logs/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: /Modo debug/ }));
    await waitFor(() => expect(body).toEqual({ debugMode: true }));
    await waitFor(() => expect(useDebugMode.getState().enabled).toBe(true));
  });
});

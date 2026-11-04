import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n';
import { renderWithProviders } from '../test/render';
import { server, useMockApi } from '../test/server';
import { DatabaseBanner } from './DatabaseBanner';

const base = 'http://127.0.0.1:9999';
const backup = 'planejamento-mensal-2026-11-01T09-30-00-000-start.db';

describe('DatabaseBanner', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('IMP-58: while a backup is open says so and goes back to the live data or restores it', async () => {
    const requests: { method: string; body: unknown }[] = [];
    let live = false;
    server.use(
      http.get(`${base}/api/database`, () => HttpResponse.json(live ? { live: true } : { live: false, backup })),
      http.put(`${base}/api/database`, async ({ request }) => {
        requests.push({ method: 'PUT', body: await request.json() });
        live = true;
        return HttpResponse.json({ live: true });
      }),
      http.post(`${base}/api/database/restores`, async ({ request }) => {
        requests.push({ method: 'POST', body: await request.json() });
        return HttpResponse.json({ live: true });
      }),
    );
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderWithProviders(<DatabaseBanner />);
    expect(await screen.findByText('Você está conferindo o backup de 01/11/2026, 09:30.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Restaurar' }));
    await waitFor(() => expect(requests).toContainEqual({ method: 'POST', body: { backup } }));
    fireEvent.click(screen.getByRole('button', { name: 'Voltar aos dados reais' }));
    await waitFor(() => expect(requests).toContainEqual({ method: 'PUT', body: {} }));
    await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
  });

  it('IMP-58: shows nothing on the live data', async () => {
    server.use(http.get(`${base}/api/database`, () => HttpResponse.json({ live: true })));
    renderWithProviders(<DatabaseBanner />);
    await new Promise((r) => setTimeout(r, 50));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});

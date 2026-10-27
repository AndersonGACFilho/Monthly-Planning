import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useNotifications } from '../../shared/state/notifications';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { SettingsPage } from './SettingsPage';

const base = 'http://127.0.0.1:9999';

describe('SettingsPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/settings`, () => HttpResponse.json({ language: '', 'alert-warning-percent': '80', 'alert-danger-percent': '100' })),
      http.get(`${base}/api/settings/smtp-password`, () => HttpResponse.json({ configured: false })),
      http.get(`${base}/api/members`, () => HttpResponse.json({})),
      http.get(`${base}/api/cards`, () => HttpResponse.json({})),
      http.get(`${base}/api/categories`, () => HttpResponse.json({})),
      http.get(`${base}/api/backups`, () => HttpResponse.json(['planejamento-mensal-2026-11-01T10-00-00-000-start.db'])),
    );
  });

  it('IMP-39: language, alerts, e-mail, spreadsheet, backups, cards and categories are all on one screen', async () => {
    renderWithProviders(<SettingsPage />);
    for (const title of ['Geral', 'E-mail (SMTP)', 'Planilha mensal', 'Cópias de segurança e dados', 'Cartões', 'Categorias']) {
      expect(await screen.findByRole('heading', { name: title })).toBeInTheDocument();
    }
    expect(screen.getByLabelText('Idioma')).toBeInTheDocument();
    expect(screen.getByText(/feche o app/)).toBeInTheDocument();
  });

  it('POR-12, RNF-07: back up now writes to data/backups; all data can be exported', async () => {
    let backup: unknown;
    let exported = '';
    server.use(
      http.post(`${base}/api/backups`, async ({ request }) => {
        backup = await request.json();
        return HttpResponse.json({ file: '/app/data/backups/copy.db' });
      }),
      http.get(`${base}/api/data-export`, ({ request }) => {
        exported = new URL(request.url).searchParams.get('format') ?? '';
        return new HttpResponse(new Blob(['{}']), { headers: { 'Content-Disposition': 'attachment; filename="dados.json"' } });
      }),
    );
    globalThis.URL.createObjectURL = () => 'blob:x';
    globalThis.URL.revokeObjectURL = () => undefined;
    renderWithProviders(<SettingsPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Fazer cópia agora' }));
    await waitFor(() => expect(backup).toEqual({}));
    await waitFor(() =>
      expect(useNotifications.getState().notices.map((n) => n.title)).toContain('Cópia salva em /app/data/backups/copy.db'),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Exportar todos os dados (CSV)' }));
    await waitFor(() => expect(exported).toBe('csv'));
  });
});

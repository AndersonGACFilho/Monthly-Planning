import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import * as desktop from '../../shared/desktop';
import { useNotifications } from '../../shared/state/notifications';
import { useUiStore } from '../../shared/state/ui';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { DatabaseExportButton } from './DatabaseExportButton';

const base = 'http://127.0.0.1:9999';

describe('DatabaseExportButton', () => {
  useMockApi();
  let formats: string[];
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    formats = [];
    useNotifications.setState({ notices: [] });
    vi.spyOn(desktop, 'downloadFile').mockImplementation(() => undefined);
    server.use(
      http.get(`${base}/api/data-export`, ({ request }) => {
        formats.push(new URL(request.url).searchParams.get('format') ?? '');
        return new HttpResponse('SQLite format 3', {
          headers: { 'Content-Disposition': 'attachment; filename="planejamento-mensal-2026-11-13.db"' },
        });
      }),
    );
  });
  afterEach(() => vi.restoreAllMocks());

  it('IMP-69: the top bar exports the whole database as one file', async () => {
    server.use(http.get(`${base}/api/database`, () => HttpResponse.json({ live: true })));
    renderWithProviders(<DatabaseExportButton />);
    fireEvent.click(await screen.findByRole('button', { name: 'Exportar banco' }));
    await waitFor(() => expect(formats).toEqual(['db']));
    expect(desktop.downloadFile).toHaveBeenCalledWith(expect.any(Blob), 'planejamento-mensal-2026-11-13.db');
    await waitFor(() =>
      expect(useNotifications.getState().notices.map((n) => n.title)).toContain(
        'Exportação concluída: planejamento-mensal-2026-11-13.db',
      ),
    );
  });

  it('IMP-69: Ctrl+Shift+E asks for the export', async () => {
    server.use(http.get(`${base}/api/database`, () => HttpResponse.json({ live: true })));
    renderWithProviders(<DatabaseExportButton />);
    await screen.findByRole('button', { name: 'Exportar banco' });
    act(() => useUiStore.getState().requestDatabaseExport());
    await waitFor(() => expect(formats).toEqual(['db']));
  });

  it('IMP-69: while a backup is open the export waits for the live data', async () => {
    server.use(http.get(`${base}/api/database`, () => HttpResponse.json({ live: false, backup: 'copia.db' })));
    renderWithProviders(<DatabaseExportButton />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Exportar banco' })).toBeDisabled());
    expect(screen.getByRole('button', { name: 'Exportar banco' })).toHaveAttribute(
      'title',
      'Volte aos dados reais para exportar o banco.',
    );
  });
});

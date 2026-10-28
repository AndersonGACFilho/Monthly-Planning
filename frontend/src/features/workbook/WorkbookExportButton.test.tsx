import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import * as desktop from '../../shared/desktop';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { WorkbookExportButton } from './WorkbookExportButton';

const base = 'http://127.0.0.1:9999';

describe('WorkbookExportButton', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-11' });
  });

  it('XLS-01, XLS-07, XLS-10: shows the default mode and the pending warning, then downloads with the chosen mode', async () => {
    let query: URLSearchParams | undefined;
    const download = vi.spyOn(desktop, 'downloadFile').mockImplementation(() => undefined);
    server.use(
      http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json({ pendingCount: 3 })),
      http.get(`${base}/api/summaries/2026-11/workbook-options`, () => HttpResponse.json({ defaultMode: 'COMPATIBLE' })),
      http.get(`${base}/api/summaries/2026-11/workbook`, ({ request }) => {
        query = new URL(request.url).searchParams;
        return new HttpResponse('xlsx', {
          headers: { 'Content-Disposition': "attachment; filename*=UTF-8''2026%20-%2011%20-%20NOVEMBRO.xlsx" },
        });
      }),
    );
    renderWithProviders(<WorkbookExportButton />);
    fireEvent.click(screen.getByRole('button', { name: /Exportar planilha/ }));
    expect(await screen.findByText(/3 lançamentos pendentes/)).toBeInTheDocument();
    expect(await screen.findByText(/Padrão deste computador: Compatível/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Modo de fórmulas'), { target: { value: 'EXCEL_365' } });
    fireEvent.click(screen.getByRole('button', { name: 'Baixar' }));
    await waitFor(() => expect(download).toHaveBeenCalled());
    expect(query?.get('mode')).toBe('EXCEL_365');
    expect(query?.get('language')).toBe('pt-BR');
    expect(download.mock.calls[0]![1]).toBe('2026 - 11 - NOVEMBRO.xlsx');
    download.mockRestore();
  });
});

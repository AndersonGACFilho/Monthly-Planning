import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { WorkbookSettings } from './WorkbookSettings';

const base = 'http://127.0.0.1:9999';

describe('WorkbookSettings', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('XLS-06, XLS-03: member sheets, card order, months and suffix are saved', async () => {
    let saved: Record<string, string> | undefined;
    server.use(
      http.get(`${base}/api/settings`, () => HttpResponse.json({ 'workbook-member-sheets': '', 'workbook-card-order': '', 'workbook-months-before': '2', 'workbook-months-after': '1', 'workbook-formula-mode': '', 'workbook-suffix': '' })),
      http.get(`${base}/api/members`, () => HttpResponse.json({ _embedded: { members: [{ id: 2, code: 'M', name: 'Mãe', active: true }] } })),
      http.get(`${base}/api/cards`, () => HttpResponse.json({ _embedded: { cards: [{ id: 7, nickname: 'Itaú' }, { id: 8, nickname: 'Azul' }] } })),
      http.patch(`${base}/api/settings`, async ({ request }) => {
        saved = (await request.json()) as Record<string, string>;
        return HttpResponse.json(saved);
      }),
    );
    renderWithProviders(<WorkbookSettings />);
    fireEvent.click(await screen.findByRole('checkbox', { name: /M — Mãe/ }));
    await screen.findByText('Azul');
    fireEvent.click(screen.getAllByRole('button', { name: 'Descer' })[0]!);
    fireEvent.change(screen.getByLabelText('Sufixo do nome do arquivo'), { target: { value: 'CJ' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(saved).toBeDefined());
    expect(saved).toMatchObject({ 'workbook-member-sheets': '2', 'workbook-card-order': '8,7', 'workbook-suffix': 'CJ' });
  });
});

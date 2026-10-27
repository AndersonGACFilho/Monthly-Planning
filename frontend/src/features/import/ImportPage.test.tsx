import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { ImportPage } from './ImportPage';

const base = 'http://127.0.0.1:9999';

const line = (id: number, status: string, decision: string, extra: Record<string, unknown> = {}) => ({
  id,
  sequence: id,
  kind: 'PURCHASE',
  purchaseDate: '2026-09-20',
  description: `LOJA ${id}`,
  installment: '-',
  amount: 10,
  status,
  decision,
  futureCheck: false,
  occurrence: 1,
  unknownFinal: false,
  _links: { edit: { href: `${base}/api/imports/1/lines/${id}` } },
  ...extra,
});

const preview = (lines: unknown[], extra: Record<string, unknown> = {}) => ({
  id: 1,
  cardId: 7,
  billingPeriod: '2026-10',
  format: 'PDF',
  fileName: 'fatura.pdf',
  status: 'PREVIEW',
  openBill: false,
  dueDate: '2026-10-09',
  fileTotal: 30,
  entriesTotal: 30,
  computedTotal: 30,
  totalDifference: 0,
  totalsMatch: true,
  warnings: [],
  newCount: 2,
  duplicateCount: 0,
  matchedCount: 0,
  divergentCount: 1,
  lines,
  _links: {
    self: { href: `${base}/api/imports/1` },
    confirm: { href: `${base}/api/imports/1/confirmation` },
    cancel: { href: `${base}/api/imports/1` },
  },
  ...extra,
});

describe('ImportPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/cards`, () =>
        HttpResponse.json({ _embedded: { cards: [{ id: 7, nickname: 'Itaú', lastDigits: ['4102'], active: true }] } }),
      ),
      http.get(`${base}/api/imports`, () => HttpResponse.json({})),
    );
  });

  it('RF-60, UI-06: uploads the file with card and period and shows the preview with divergent lines expanded', async () => {
    let query = '';
    server.use(
      http.post(`${base}/api/imports`, ({ request }) => {
        query = new URL(request.url).search;
        return HttpResponse.json(preview([line(1, 'NEW', 'IMPORT'), line(2, 'NEW', 'IMPORT'), line(3, 'DIVERGENT', 'UNDECIDED')]), { status: 201 });
      }),
    );
    renderWithProviders(<ImportPage />);
    await screen.findByRole('option', { name: 'Itaú (4102)' });
    fireEvent.change(screen.getByLabelText('Competência'), { target: { value: '2026-10' } });
    const file = new File(['%PDF'], 'fatura.pdf', { type: 'application/pdf' });
    fireEvent.change(screen.getByTestId('import-file'), { target: { files: [file] } });
    expect(await screen.findByRole('heading', { name: 'fatura.pdf' })).toBeInTheDocument();
    expect(query).toContain('cardId=7');
    expect(query).toContain('billingPeriod=2026-10');
    expect(screen.getByText('LOJA 3')).toBeInTheDocument();
    expect(screen.queryByText('LOJA 1')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Confirmar importação' })).toBeDisabled();
  });

  it('RF-69, RF-72: deciding the divergent line enables confirmation, which posts the acknowledgement', async () => {
    let confirmed: unknown;
    server.use(
      http.post(`${base}/api/imports`, () =>
        HttpResponse.json(preview([line(3, 'DIVERGENT', 'UNDECIDED')]), { status: 201 }),
      ),
      http.patch(`${base}/api/imports/1/lines/3`, () => HttpResponse.json(preview([line(3, 'DIVERGENT', 'IGNORE')]))),
      http.post(`${base}/api/imports/1/confirmation`, async ({ request }) => {
        confirmed = await request.json();
        return HttpResponse.json(preview([line(3, 'DIVERGENT', 'IGNORE')], { status: 'CONFIRMED', _links: { self: { href: `${base}/api/imports/1` } } }));
      }),
    );
    renderWithProviders(<ImportPage />);
    await screen.findByRole('option', { name: 'Itaú (4102)' });
    fireEvent.change(screen.getByTestId('import-file'), { target: { files: [new File(['x'], 'fatura.pdf')] } });
    const row = (await screen.findByText('LOJA 3')).closest('tr')!;
    fireEvent.change(within(row).getByRole('combobox'), { target: { value: 'IGNORE' } });
    await waitFor(() => expect(screen.getByRole('button', { name: 'Confirmar importação' })).toBeEnabled());
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar importação' }));
    await waitFor(() => expect(confirmed).toEqual({ acknowledgeTotals: false }));
    expect(await screen.findByText(/Confirmada/)).toBeInTheDocument();
  });

  it('RF-62, RF-71: warnings must be acknowledged before confirming', async () => {
    server.use(
      http.post(`${base}/api/imports`, () =>
        HttpResponse.json(preview([line(1, 'NEW', 'IMPORT')], { warnings: ['period-mismatch'], divergentCount: 0, totalDifference: 1.5 }), { status: 201 }),
      ),
    );
    renderWithProviders(<ImportPage />);
    await screen.findByRole('option', { name: 'Itaú (4102)' });
    fireEvent.change(screen.getByTestId('import-file'), { target: { files: [new File(['x'], 'fatura.pdf')] } });
    expect(await screen.findByText('O vencimento da fatura é de outra competência.')).toBeInTheDocument();
    const confirm = screen.getByRole('button', { name: 'Confirmar importação' });
    expect(confirm).toBeDisabled();
    fireEvent.click(screen.getByRole('checkbox'));
    expect(confirm).toBeEnabled();
  });
});

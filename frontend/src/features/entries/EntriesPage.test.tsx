import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { EntriesPage } from './EntriesPage';

const base = 'http://127.0.0.1:9999';

const entry = (id: number, description: string, amount: number, extra: Record<string, unknown> = {}) => ({
  id,
  purchaseId: id,
  cardId: 7,
  billingPeriod: '2026-10',
  entryDate: `2026-09-${String(id).padStart(2, '0')}`,
  purchaseDate: `2026-09-${String(id).padStart(2, '0')}`,
  originalDescription: description,
  displayDescription: description,
  installment: '-',
  type: 'SINGLE',
  kind: 'PURCHASE',
  amount,
  status: 'CONFIRMED',
  splits: [{ memberId: 1, percentage: 100 }],
  reviewStatus: 'REVIEWED',
  refundStatus: 'NONE',
  version: 0,
  _links: {
    self: { href: `${base}/api/bill-entries/${id}` },
    purchase: { href: `${base}/api/purchases/${id}` },
    rename: { href: `${base}/api/bill-entries/${id}/alias` },
  },
  ...extra,
});

describe('EntriesPage', () => {
  useMockApi();
  let lastQuery = '';
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-10' });
    server.use(
      http.get(`${base}/api/bill-entries`, ({ request }) => {
        lastQuery = new URL(request.url).search;
        return HttpResponse.json({
          _embedded: {
            billEntries: [
              entry(1, 'PADARIA', 12.5),
              entry(2, 'POSTO AGRONOMIA', 50, {
                displayDescription: 'Posto do bairro',
                _links: {
                  self: { href: `${base}/api/bill-entries/2` },
                  purchase: { href: `${base}/api/purchases/2` },
                  rename: { href: `${base}/api/bill-entries/2/alias` },
                  'restore-original': { href: `${base}/api/bill-entries/2/alias` },
                },
              }),
              entry(3, 'ESTORNO LOJA', -20),
            ],
          },
        });
      }),
      http.get(`${base}/api/cards`, () => HttpResponse.json({})),
      http.get(`${base}/api/members`, () => HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }] } })),
      http.get(`${base}/api/categories`, () => HttpResponse.json({ _embedded: { categories: [{ id: 5, name: 'Mercado' }] } })),
      http.get(`${base}/api/purchases/:id/installments`, () => HttpResponse.json({})),
    );
  });

  it('RF-40, RF-42: lists the entries of the billing period with the quick total of filtered rows', async () => {
    renderWithProviders(<EntriesPage />);
    expect(await screen.findByText('PADARIA')).toBeInTheDocument();
    expect(lastQuery).toContain('billingPeriod=2026-10');
    const bar = screen.getByRole('status');
    expect(bar).toHaveTextContent('3 filtrados');
    expect(bar.textContent?.replace(/\u00a0/g, ' ')).toContain('R$ 42,50');
  });

  it('RF-38, UI-02: a billing period in the link becomes the global period', async () => {
    useBillingPeriodStore.setState({ period: '2026-09' });
    const { MemoryRouter } = await import('react-router');
    const { QueryClientProvider } = await import('@tanstack/react-query');
    const { createQueryClient } = await import('../../app/queryClient');
    const { render } = await import('@testing-library/react');
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter initialEntries={['/entries?billingPeriod=2026-11&card=7']}>
          <EntriesPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await waitFor(() => expect(useBillingPeriodStore.getState().period).toBe('2026-11'));
    await waitFor(() => expect(lastQuery).toContain('card=7'));
  });

  it('RF-40, API-06: filters go to the API as query parameters', async () => {
    renderWithProviders(<EntriesPage />);
    await screen.findByText('PADARIA');
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'n/N' } });
    await waitFor(() => expect(lastQuery).toContain('type=n%2FN'));
  });

  it('UI-04, RF-42: Ctrl+click selects rows and the bar sums the selection', async () => {
    renderWithProviders(<EntriesPage />);
    const padaria = (await screen.findByText('PADARIA')).closest('tr')!;
    fireEvent.click(within(padaria).getByText('-'));
    const refund = screen.getByText('ESTORNO LOJA').closest('tr')!;
    fireEvent.click(within(refund).getByText('-'), { ctrlKey: true });
    const bar = screen.getByRole('status');
    expect(bar).toHaveTextContent('2 selecionados');
    expect(bar.textContent?.replace(/\u00a0/g, ' ')).toContain('-R$ 7,50');
  });

  it('RF-46, RF-48, RF-50: renames inline for every entry or only this one, and offers restore only with an alias', async () => {
    let renamed: unknown;
    server.use(
      http.put(`${base}/api/bill-entries/1/alias`, async ({ request }) => {
        renamed = await request.json();
        return HttpResponse.json(entry(1, 'PADARIA', 12.5));
      }),
    );
    renderWithProviders(<EntriesPage />);
    expect(await screen.findAllByRole('button', { name: 'Restaurar original' })).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: 'PADARIA' }));
    const input = screen.getByLabelText('Nome exibido');
    fireEvent.change(input, { target: { value: 'Padaria da esquina' } });
    fireEvent.click(screen.getByLabelText('Só este lançamento'));
    fireEvent.keyDown(input, { key: 'Enter' });
    await waitFor(() => expect(renamed).toEqual({ displayName: 'Padaria da esquina', scope: 'ONLY_THIS' }));
  });

  it('RF-44: a manual entry goes to the chosen bill and the page follows it there', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/cards`, () => HttpResponse.json({ _embedded: { cards: [{ id: 7, nickname: 'Itaú', active: true }] } })),
      http.post(`${base}/api/bill-entries`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(entry(9, 'Geladeira', 250, { billingPeriod: '2026-11' }), { status: 201 });
      }),
    );
    renderWithProviders(<EntriesPage />);
    fireEvent.click(await screen.findByRole('button', { name: /Novo lançamento/ }));
    const form = screen.getByRole('form', { name: 'Lançamento manual' });
    await within(form).findByRole('option', { name: 'Itaú' });
    expect(within(form).getByLabelText('Fatura (competência)')).toHaveValue('2026-10');
    fireEvent.change(within(form).getByLabelText('Fatura (competência)'), { target: { value: '2026-11' } });
    fireEvent.change(within(form).getByLabelText('Descrição'), { target: { value: 'Geladeira' } });
    fireEvent.change(within(form).getByLabelText('Valor da parcela'), { target: { value: '250,00' } });
    fireEvent.submit(form);

    await waitFor(() => expect(useBillingPeriodStore.getState().period).toBe('2026-11'));
    expect(body).toMatchObject({ cardId: 7, description: 'Geladeira', amount: 250, installmentCount: 1, billingPeriod: '2026-11' });
    expect(screen.queryByRole('form', { name: 'Lançamento manual' })).not.toBeInTheDocument();
  });

  it('RF-44, API-09: a manual entry offers edit and delete; the edit replaces its purchase', async () => {
    let replaced: { body: unknown; ifMatch: string | null } | undefined;
    let deleted = false;
    const manualLinks = (id: number) => ({
      self: { href: `${base}/api/bill-entries/${id}` },
      purchase: { href: `${base}/api/purchases/${id}` },
      rename: { href: `${base}/api/bill-entries/${id}/alias` },
      'replace-purchase': { href: `${base}/api/bill-entries/${id}/purchase` },
      delete: { href: `${base}/api/bill-entries/${id}` },
    });
    server.use(
      http.get(`${base}/api/cards`, () => HttpResponse.json({ _embedded: { cards: [{ id: 7, nickname: 'Itaú', active: true }] } })),
      http.get(`${base}/api/bill-entries`, () =>
        HttpResponse.json({
          _embedded: { billEntries: [entry(1, 'PADARIA', 12.5), entry(4, 'Sofá', 300, { version: 3, _links: manualLinks(4) })] },
        }),
      ),
      http.put(`${base}/api/bill-entries/4/purchase`, async ({ request }) => {
        replaced = { body: await request.json(), ifMatch: request.headers.get('If-Match') };
        return HttpResponse.json(entry(4, 'Sofá', 350, { _links: manualLinks(4) }));
      }),
      http.delete(`${base}/api/bill-entries/4`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderWithProviders(<EntriesPage />);

    fireEvent.click(within((await screen.findByText('PADARIA')).closest('tr')!).getByText('-'));
    const imported = screen.getByRole('complementary', { name: 'Detalhes do lançamento' });
    expect(within(imported).queryByRole('button', { name: 'Editar' })).not.toBeInTheDocument();
    expect(within(imported).queryByRole('button', { name: 'Excluir' })).not.toBeInTheDocument();

    fireEvent.click(within(screen.getByText('Sofá').closest('tr')!).getByText('-'));
    const details = screen.getByRole('complementary', { name: 'Detalhes do lançamento' });
    fireEvent.click(within(details).getByRole('button', { name: 'Editar' }));
    const form = screen.getByRole('form', { name: 'Editar lançamento manual' });
    expect(within(form).getByLabelText('Descrição')).toHaveValue('Sofá');
    fireEvent.change(within(form).getByLabelText('Valor da parcela'), { target: { value: '350,00' } });
    fireEvent.submit(form);
    await waitFor(() => expect(replaced?.ifMatch).toBe('"3"'));
    expect(replaced?.body).toMatchObject({ cardId: 7, description: 'Sofá', amount: 350, billingPeriod: '2026-10' });

    fireEvent.click(within(details).getByRole('button', { name: 'Excluir' }));
    await waitFor(() => expect(deleted).toBe(true));
    confirm.mockRestore();
  });

  it('RF-26, RF-43: the details panel splits the purchase by amount and saves it', async () => {
    let patched: { body: unknown; ifMatch: string | null } | undefined;
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }, { id: 2, code: 'M', name: 'Mãe', active: true }] },
        }),
      ),
      http.patch(`${base}/api/bill-entries/1`, async ({ request }) => {
        patched = { body: await request.json(), ifMatch: request.headers.get('If-Match') };
        return HttpResponse.json(entry(1, 'PADARIA', 12.5));
      }),
    );
    renderWithProviders(<EntriesPage />);
    fireEvent.click(within((await screen.findByText('PADARIA')).closest('tr')!).getByText('-'));
    const split = within(screen.getByRole('region', { name: 'Rateio' }));
    fireEvent.click(split.getByRole('button', { name: /Dividir$/ }));
    await split.findAllByRole('option', { name: 'M — Mãe' });
    fireEvent.change(split.getAllByLabelText('Conta')[1]!, { target: { value: '2' } });
    fireEvent.click(split.getByRole('button', { name: 'Dividir por valor' }));
    fireEvent.click(split.getByRole('button', { name: 'Salvar rateio' }));
    await waitFor(() => expect(patched?.ifMatch).toBe('"0"'));
    expect(patched?.body).toMatchObject({ splits: [{ memberId: 1, amount: 6.25 }, { memberId: 2, amount: 6.25 }] });
  });
});

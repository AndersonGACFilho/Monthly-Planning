import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { ReviewQueuePage } from './ReviewQueuePage';

const base = 'http://127.0.0.1:9999';

const item = (id: number, description: string, extra: Record<string, unknown> = {}) => ({
  purchaseId: id,
  entryId: id * 10,
  cardId: 7,
  billingPeriod: '2026-10',
  purchaseDate: '2026-09-20',
  originalDescription: description,
  installment: '-',
  amount: 12.5,
  kind: 'PURCHASE',
  splits: [{ memberId: 1, percentage: 100 }],
  version: 0,
  subscriptionSuggested: false,
  _links: { validate: { href: `${base}/api/review-queue/${id}/validation` } },
  ...extra,
});

describe('ReviewQueuePage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/cards`, () => HttpResponse.json({})),
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }] } }),
      ),
      http.get(`${base}/api/categories`, () =>
        HttpResponse.json({ _embedded: { categories: [{ id: 5, name: 'Mercado' }] } }),
      ),
    );
  });

  it('RF-74, RF-79: lists pending items with their count and total', async () => {
    server.use(
      http.get(`${base}/api/review-queue`, () =>
        HttpResponse.json({ _embedded: { items: [item(1, 'PADARIA'), item(2, 'FARMACIA')] } }),
      ),
    );
    renderWithProviders(<ReviewQueuePage />);
    expect(await screen.findByText('PADARIA')).toBeInTheDocument();
    expect(screen.getByText(/2 pendentes/)).toBeInTheDocument();
  });

  it('RF-76, RF-78: Enter validates the item with its category and moves on', async () => {
    let body: Record<string, unknown> | undefined;
    let ifMatch: string | null = null;
    server.use(
      http.get(`${base}/api/review-queue`, () =>
        HttpResponse.json({ _embedded: { items: [item(1, 'PADARIA'), item(2, 'FARMACIA')] } }),
      ),
      http.post(`${base}/api/review-queue/1/validation`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        ifMatch = request.headers.get('If-Match');
        return HttpResponse.json(item(1, 'PADARIA'));
      }),
    );
    renderWithProviders(<ReviewQueuePage />);
    await screen.findByText('PADARIA');
    await screen.findAllByRole('option', { name: 'Mercado' });
    const category = screen.getAllByLabelText('Categoria')[0]!;
    fireEvent.change(category, { target: { value: '5' } });
    fireEvent.keyDown(category, { key: 'Enter' });
    await waitFor(() => expect(body).toMatchObject({ categoryId: 5, splits: [{ memberId: 1, percentage: 100 }] }));
    expect(ifMatch).toBe('"0"');
  });

  it('RF-77: selected items get the same category and are validated together', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/review-queue`, () =>
        HttpResponse.json({
          _embedded: { items: [item(1, 'PADARIA'), item(2, 'FARMACIA')] },
          _links: { 'validate-selected': { href: `${base}/api/review-queue/validations` } },
        }),
      ),
      http.post(`${base}/api/review-queue/validations`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({});
      }),
    );
    renderWithProviders(<ReviewQueuePage />);
    await screen.findByText('PADARIA');
    fireEvent.click(screen.getByLabelText('Selecionar todos'));
    await screen.findAllByRole('option', { name: 'Mercado' });
    fireEvent.change(screen.getByLabelText('Categoria', { selector: '#batch-category' }), { target: { value: '5' } });
    fireEvent.click(screen.getByRole('button', { name: 'Validar selecionados' }));
    await waitFor(() => expect(body).toMatchObject({ purchaseIds: [1, 2], validate: true, changes: { categoryId: 5 } }));
  });
});

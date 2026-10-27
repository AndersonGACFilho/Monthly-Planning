import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { ExpensesPage } from './ExpensesPage';

const base = 'http://127.0.0.1:9999';

const bill = (extra: Record<string, unknown> = {}) => ({
  cardId: 7,
  nickname: 'Itaú Platinum',
  lastDigits: ['4102'],
  billingPeriod: '2026-11',
  closingDate: '2026-11-03',
  dueDate: '2026-11-10',
  status: 'CLOSED',
  partial: false,
  total: 1500.5,
  single: 1000,
  installments: 400.5,
  subscriptions: 100,
  committedFuture: 800,
  unconfirmedCount: 1,
  pendingReviewCount: 0,
  version: 3,
  _links: { 'mark-paid': { href: `${base}/api/cards/7/bills/2026-11/payment` } },
  ...extra,
});

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path="/" element={<ExpensesPage />} />
      <Route path="/entries" element={<p>entries tab</p>} />
    </Routes>,
  );
}

describe('ExpensesPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-11' });
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }] } }),
      ),
      http.get(`${base}/api/categories`, () => HttpResponse.json({})),
      http.get(`${base}/api/recurrence-rules`, () => HttpResponse.json({})),
      http.get(`${base}/api/occurrences`, () => HttpResponse.json({})),
      http.get(`${base}/api/card-bills`, () => HttpResponse.json({ _embedded: { cardBills: [bill()] } })),
    );
  });

  it('RF-36, RF-37: a card shows total, dates, status and the split; a closed bill can be paid', async () => {
    let ifMatch: string | null = null;
    server.use(
      http.get(`${base}/api/variable-expenses`, () => HttpResponse.json({ items: [], total: 0 })),
      http.post(`${base}/api/cards/7/bills/2026-11/payment`, ({ request }) => {
        ifMatch = request.headers.get('If-Match');
        return HttpResponse.json(bill({ status: 'PAID', _links: {} }));
      }),
    );
    renderPage();
    expect(await screen.findByText('Fechada')).toBeInTheDocument();
    expect(screen.getByText(/Fecha em/)).toBeInTheDocument();
    expect(screen.getByText('Parcelas')).toBeInTheDocument();
    expect(screen.getByText(/1 não confirmado/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Marcar fatura como paga/ }));
    await waitFor(() => expect(ifMatch).toBe('"3"'));
  });

  it('RF-38: clicking a card total opens the card entries tab', async () => {
    server.use(http.get(`${base}/api/variable-expenses`, () => HttpResponse.json({ items: [], total: 0 })));
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Ver lançamentos de Itaú Platinum' }));
    expect(await screen.findByText('entries tab')).toBeInTheDocument();
  });

  it('RF-34, RF-35: variable expenses are filtered by member with the filter total', async () => {
    const members: (string | null)[] = [];
    server.use(
      http.get(`${base}/api/variable-expenses`, ({ request }) => {
        members.push(new URL(request.url).searchParams.get('member'));
        return HttpResponse.json({
          items: [{ id: 9, expenseDate: '2026-11-03', billingPeriod: '2026-11', description: 'Feira', memberId: 1, amount: 120.5, paymentMethod: 'PIX', version: 0 }],
          total: 120.5,
        });
      }),
    );
    renderPage();
    expect(await screen.findByText('Feira')).toBeInTheDocument();
    expect(screen.getByText(/Total do filtro/)).toHaveTextContent('120,50');
    await screen.findAllByRole('option', { name: 'AB — Casa' });
    fireEvent.change(screen.getAllByLabelText('Conta')[0]!, { target: { value: '1' } });
    await waitFor(() => expect(members).toContain('1'));
  });
});

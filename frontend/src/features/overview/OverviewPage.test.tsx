import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useLocation } from 'react-router';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { OverviewPage } from './OverviewPage';

const base = 'http://127.0.0.1:9999';

/** Shows where the router is, to check the links of the page. */
function Where() {
  const location = useLocation();
  return <p data-testid="where">{location.pathname + location.search}</p>;
}

const summary = (extra: Record<string, unknown> = {}) => ({
  billingPeriod: '2026-11',
  income: 10250,
  expense: 8500,
  balance: 1750,
  percentSpent: 82.9,
  alert: 'WARNING',
  mainIncome: 10000,
  reimbursements: 250,
  fixedTotal: 2000,
  variableTotal: 300,
  cardTotal: 6200,
  categories: [
    { categoryId: 5, amount: 6000, percentOfTotal: 70.6, previousAmount: 5000, variation: 20 },
    { amount: 2500, percentOfTotal: 29.4, previousAmount: 0 },
  ],
  members: [{ memberId: 1, main: true, income: 10000, fixed: 2000, variable: 300, card: 5950, expense: 8250, balance: 1750, percentSpent: 82.5 }],
  cards: [{ cardId: 7, nickname: 'Itaú Platinum', total: 6200, committedFuture: 900 }],
  committedNext: [
    { billingPeriod: '2026-12', amount: 500 },
    { billingPeriod: '2027-01', amount: 300 },
    { billingPeriod: '2027-02', amount: 100 },
  ],
  settlements: [{ fromMemberId: 2, toMemberId: 1, amount: 250 }],
  pendingCount: 3,
  pendingAmount: 410,
  unconfirmedCount: 0,
  ...extra,
});

describe('OverviewPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-11' });
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: {
            members: [
              { id: 1, code: 'AB', name: 'Casa', active: true },
              { id: 2, code: 'M', name: 'Mãe', active: true },
            ],
          },
        }),
      ),
      http.get(`${base}/api/categories`, () =>
        HttpResponse.json({
          _embedded: {
            categories: [
              { id: 5, name: 'Mercado', color: '#16a34a', icon: 'shopping-cart' },
              { id: 1, name: 'Não classificado', color: '#9aa4af', icon: 'circle-help', system: true },
            ],
          },
        }),
      ),
      http.get(`${base}/api/settings`, () => HttpResponse.json({ 'alert-warning-percent': '80', 'alert-danger-percent': '100' })),
    );
  });

  it('RF-10, RF-16: indicators and the yellow alert above the configured threshold', async () => {
    server.use(http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())));
    renderWithProviders(<OverviewPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('80%');
    expect(screen.getByText('82,9%')).toBeInTheDocument();
    expect(screen.getByText(/de reembolsos/)).toBeInTheDocument();
  });

  it('RF-12, UI-07: category chart and table with share and variation', async () => {
    server.use(http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())));
    renderWithProviders(<OverviewPage />);
    expect(await screen.findByLabelText('Gráfico de despesas por categoria')).toBeInTheDocument();
    expect(await screen.findByRole('rowheader', { name: /Mercado/ })).toBeInTheDocument();
    expect(screen.getByText('+20,0%')).toBeInTheDocument();
    expect(screen.getByText('Não classificado')).toBeInTheDocument();
  });

  it('RF-17, RF-27, RF-79a: committed months, who owes whom and pending entries', async () => {
    server.use(http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())));
    renderWithProviders(<OverviewPage />);
    expect(await screen.findByText(/3 lançamentos pendentes de validação/)).toBeInTheDocument();
    expect(await screen.findByText(/M deve R\$\s250,00 a AB/)).toBeInTheDocument();
    expect(screen.getByText('Comprometido nos próximos meses')).toBeInTheDocument();
  });

  it('RF-15: the member filter asks for that member only', async () => {
    const members: (string | null)[] = [];
    server.use(
      http.get(`${base}/api/summaries/2026-11`, ({ request }) => {
        members.push(new URL(request.url).searchParams.get('member'));
        return HttpResponse.json(summary({ alert: 'NONE', pendingCount: 0 }));
      }),
    );
    renderWithProviders(<OverviewPage />);
    await screen.findAllByRole('option', { name: 'M — Mãe' });
    fireEvent.change(screen.getByLabelText('Ver só a conta'), { target: { value: '2' } });
    await waitFor(() => expect(members).toContain('2'));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('RF-12, RF-38: a category opens its entries, and expenses without one open "not classified"', async () => {
    server.use(http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())));
    renderWithProviders(
      <>
        <OverviewPage />
        <Where />
      </>,
    );
    fireEvent.click(await screen.findByRole('button', { name: 'Mercado' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/entries?billingPeriod=2026-11&category=5');
    fireEvent.click(await screen.findByRole('button', { name: 'Não classificado' }));
    expect(screen.getByTestId('where')).toHaveTextContent('/entries?billingPeriod=2026-11&category=1');
  });
});

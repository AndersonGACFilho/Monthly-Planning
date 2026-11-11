import { fireEvent, screen, waitFor, within } from '@testing-library/react';
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

const history = {
  from: '2026-06',
  to: '2026-11',
  points: [
    { billingPeriod: '2026-10', income: 10000, expense: 9000, balance: 1000 },
    { billingPeriod: '2026-11', income: 10250, expense: 8500, balance: 1750 },
  ],
};

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
      http.get(`${base}/api/history`, () => HttpResponse.json(history)),
    );
  });

  it('RF-10: what was left this month, against the last six months, and a month opens from the chart', async () => {
    const asked: string[] = [];
    server.use(
      http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())),
      http.get(`${base}/api/history`, ({ request }) => {
        asked.push(new URL(request.url).search);
        return HttpResponse.json(history);
      }),
    );
    renderWithProviders(<OverviewPage />);
    const section = await screen.findByRole('region', { name: 'Quanto sobrou em novembro de 2026' });
    expect(await within(section).findByText('17,1% da renda')).toBeInTheDocument();
    expect(within(section).getByRole('img', { name: 'Gasto 82,9% da renda, sobrou 17,1%' })).toBeInTheDocument();
    expect(within(section).getByLabelText(/Sobra dos últimos 6 meses/)).toBeInTheDocument();
    await waitFor(() => expect(asked).toContain('?from=2026-06&to=2026-11'));
  });

  it('RF-10, RF-16: indicators and the yellow alert above the configured threshold', async () => {
    server.use(http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())));
    renderWithProviders(<OverviewPage />);
    expect(await screen.findByRole('alert')).toHaveTextContent('80%');
    expect(screen.getByText('82,9%')).toBeInTheDocument();
    expect(screen.getByText(/de reembolsos/)).toBeInTheDocument();
  });

  it('IMP-65: the expense shows the subscriptions and subscription categories are marked', async () => {
    server.use(
      http.get(`${base}/api/summaries/2026-11`, () =>
        HttpResponse.json(summary({ subscriptionTotal: 99.8, categories: [{ categoryId: 6, amount: 99.8, percentOfTotal: 100 }] })),
      ),
      http.get(`${base}/api/categories`, () =>
        HttpResponse.json({ _embedded: { categories: [{ id: 6, name: 'Streaming', color: '#6366f1', icon: 'repeat', subscription: true }] } }),
      ),
    );
    renderWithProviders(<OverviewPage />);
    expect(await screen.findByText(/Assinaturas R\$\s99,80/)).toBeInTheDocument();
    const row = await screen.findByRole('rowheader', { name: /Streaming/ });
    expect(within(row).getByText('Assinatura')).toBeInTheDocument();
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

  it('RF-12, RF-38: a category lists its fixed, variable and card items and opens the card entries', async () => {
    const asked: (string | null)[] = [];
    server.use(
      http.get(`${base}/api/summaries/2026-11`, () => HttpResponse.json(summary())),
      http.get(`${base}/api/summaries/2026-11/category-items`, ({ request }) => {
        const category = new URL(request.url).searchParams.get('category');
        asked.push(category);
        return HttpResponse.json(
          category === '5'
            ? [
                { source: 'FIXED', referenceId: 1, description: 'Cesta básica', date: '2026-11-05', amount: 300, memberIds: [1] },
                { source: 'VARIABLE', referenceId: 2, description: 'Feira', date: '2026-11-09', amount: 35, memberIds: [1] },
                { source: 'CARD', referenceId: 3, description: 'Supermercado', date: '2026-11-12', installment: '1/2', cardId: 7, amount: 100, memberIds: [1, 2] },
              ]
            : [{ source: 'VARIABLE', referenceId: 4, description: 'Sem categoria', date: '2026-11-01', amount: 8, memberIds: [1] }],
        );
      }),
    );
    renderWithProviders(
      <>
        <OverviewPage />
        <Where />
      </>,
    );
    fireEvent.click(await screen.findByRole('button', { name: 'Mercado' }));
    const dialog = await screen.findByRole('dialog', { name: 'Itens de Mercado' });
    expect(await within(dialog).findByText('Cesta básica')).toBeInTheDocument();
    expect(within(dialog).getByRole('heading', { name: 'Despesas fixas' })).toBeInTheDocument();
    expect(within(dialog).getByRole('heading', { name: 'Despesas dinâmicas' })).toBeInTheDocument();
    expect(within(dialog).getByText('AB, M')).toBeInTheDocument();
    expect(within(dialog).getByText('Total de 3 itens')).toBeInTheDocument();
    expect(within(dialog).getByText(/435,00/)).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: /Abrir lançamentos/ }));
    expect(screen.getByTestId('where')).toHaveTextContent('/entries?billingPeriod=2026-11&category=5');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    fireEvent.click(await screen.findByRole('button', { name: 'Não classificado' }));
    expect(await screen.findByText('Sem categoria')).toBeInTheDocument();
    expect(asked).toEqual(['5', null]);
  });
});

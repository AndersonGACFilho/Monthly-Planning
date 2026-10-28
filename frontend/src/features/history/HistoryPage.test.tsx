import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { HistoryPage } from './HistoryPage';

const base = 'http://127.0.0.1:9999';

const month = (billingPeriod: string, income: number, expense: number) => ({
  billingPeriod,
  income,
  mainIncome: income,
  reimbursements: 0,
  expense,
  fixed: 0,
  variable: 0,
  card: expense,
  balance: income - expense,
  byCategory: { '6': expense / 2, '9': expense / 2 },
  byMember: { '1': expense },
});

describe('HistoryPage', () => {
  useMockApi();
  const requests: URL[] = [];
  beforeEach(async () => {
    requests.length = 0;
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-10' });
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }] } }),
      ),
      http.get(`${base}/api/categories`, () =>
        HttpResponse.json({
          _embedded: {
            categories: [
              { id: 5, name: 'Moradia', color: '#16a34a', icon: 'house' },
              { id: 6, name: 'Mercado', color: '#22c55e', icon: 'cart', parentId: 5 },
              { id: 9, name: 'Lazer', color: '#7c3aed', icon: 'film' },
            ],
          },
        }),
      ),
      http.get(`${base}/api/history`, ({ request }) => {
        const url = new URL(request.url);
        requests.push(url);
        return HttpResponse.json({
          from: url.searchParams.get('from'),
          to: url.searchParams.get('to'),
          points: [month('2026-05', 1000, 400), month('2026-06', 1000, 600), month('2026-07', 0, 100)],
        });
      }),
    );
  });

  it('RF-10: the last 12 months by default, grouped by semester on demand', async () => {
    renderWithProviders(<HistoryPage />);
    expect(await screen.findByRole('heading', { name: 'Histórico' })).toBeInTheDocument();
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('from')).toBe('2025-11'));
    expect(requests.at(-1)?.searchParams.get('to')).toBe('2026-10');
    const table = await screen.findByRole('table', { name: 'Totais por período' });
    expect(await within(table).findByRole('rowheader', { name: 'mai/26' })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Agrupar por'), { target: { value: 'semester' } });
    expect(within(table).getByRole('rowheader', { name: '1º sem 2026' }).closest('tr')).toHaveTextContent('50,0%');
    expect(within(table).getByRole('rowheader', { name: '2º sem 2026' }).closest('tr')).toHaveTextContent('—');
    expect(within(table).getByRole('rowheader', { name: 'Total' }).closest('tr')).toHaveTextContent('R$ 900,00');
  });

  it('RF-12, RF-15: totals by category roll up to the parent; filters go to the API', async () => {
    renderWithProviders(<HistoryPage />);
    const byCategory = await screen.findByRole('table', { name: 'Total por categoria no período' });
    expect(await within(byCategory).findByRole('rowheader', { name: 'Moradia' })).toBeInTheDocument();
    expect(within(screen.getByRole('table', { name: 'Total por conta no período' })).getByRole('rowheader', { name: 'Casa' }))
      .toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('Período'), { target: { value: 'lastYear' } });
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: '1' } });
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('member')).toBe('1'));
    expect(requests.at(-1)?.searchParams.get('from')).toBe('2025-01');
    fireEvent.change(screen.getByLabelText('Categoria'), { target: { value: '5' } });
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('category')).toBe('5'));
    const filtered = () => screen.getByRole('table', { name: 'Total por categoria no período' });
    await waitFor(() => expect(within(filtered()).getByRole('rowheader', { name: 'Mercado' })).toBeInTheDocument());
    expect(within(filtered()).queryByRole('rowheader', { name: 'Lazer' })).not.toBeInTheDocument();
  });

  it('RF-15: a custom range that goes backwards is rejected without a request', async () => {
    renderWithProviders(<HistoryPage />);
    await waitFor(() => expect(requests).toHaveLength(1));
    fireEvent.change(screen.getByLabelText('Período'), { target: { value: 'custom' } });
    fireEvent.change(screen.getByLabelText('De'), { target: { value: '2026-12' } });
    expect(await screen.findByRole('alert')).toHaveTextContent('O início precisa vir antes do fim');
    expect(requests.every((url) => url.searchParams.get('from') !== '2026-12')).toBe(true);
  });
});

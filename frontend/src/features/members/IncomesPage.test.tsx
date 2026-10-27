import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { IncomesPage } from './IncomesPage';

const base = 'http://127.0.0.1:9999';

const salary = (extra: Record<string, unknown> = {}) => ({
  id: 3,
  ruleId: 1,
  kind: 'INCOME',
  description: 'Salário',
  memberId: 1,
  billingPeriod: '2026-11',
  day: 5,
  amount: 5000,
  defaultAmount: 5000,
  amountOverridden: false,
  status: 'EXPECTED',
  version: 2,
  _links: {
    'override-amount': { href: `${base}/api/occurrences/3/amount` },
    'mark-received': { href: `${base}/api/occurrences/3/status` },
  },
  ...extra,
});

describe('IncomesPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-11' });
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true, color: '#000' }] } }),
      ),
      http.get(`${base}/api/categories`, () => HttpResponse.json({})),
      http.get(`${base}/api/recurrence-rules`, () => HttpResponse.json({})),
      http.get(`${base}/api/summaries/2026-11`, () =>
        HttpResponse.json({
          members: [{ memberId: 1, main: true, income: 5000, fixed: 2000, variable: 300, card: 700, expense: 3000, balance: 2000, percentSpent: 60 }],
        }),
      ),
    );
  });

  it('RF-22, RF-24: lists the incomes of the period and marks one received', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/occurrences`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('billingPeriod')).toBe('2026-11');
        return HttpResponse.json({ _embedded: { occurrences: [salary()] } });
      }),
      http.post(`${base}/api/occurrences/3/status`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(salary({ status: 'DONE', effectiveDate: '2026-11-05' }));
      }),
    );
    renderWithProviders(<IncomesPage />);
    expect(await screen.findByText('Salário')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Marcar como recebida/ }));
    await waitFor(() => expect(body).toEqual({ done: true }));
  });

  it('RF-22: the amount of one month changes with If-Match and keeps the default', async () => {
    let body: Record<string, unknown> | undefined;
    let ifMatch: string | null = null;
    server.use(
      http.get(`${base}/api/occurrences`, () => HttpResponse.json({ _embedded: { occurrences: [salary()] } })),
      http.put(`${base}/api/occurrences/3/amount`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        ifMatch = request.headers.get('If-Match');
        return HttpResponse.json(salary({ amount: 4800, amountOverridden: true }));
      }),
    );
    renderWithProviders(<IncomesPage />);
    await screen.findByText('Salário');
    fireEvent.click(screen.getAllByRole('button', { name: 'Alterar valor só deste mês' })[0]!);
    const input = screen.getByRole('textbox', { name: 'Alterar valor só deste mês' });
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: '4.800,00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(body).toEqual({ amount: 4800 }));
    expect(ifMatch).toBe('"2"');
  });

  it('RF-25: shows the total expense by member', async () => {
    server.use(http.get(`${base}/api/occurrences`, () => HttpResponse.json({})));
    renderWithProviders(<IncomesPage />);
    expect(await screen.findByRole('table', { name: 'Despesa total por conta' })).toBeInTheDocument();
    expect(await screen.findByText('60,0%')).toBeInTheDocument();
  });

  it('RF-20: the members registry is reachable from the incomes tab', async () => {
    server.use(http.get(`${base}/api/occurrences`, () => HttpResponse.json({})));
    renderWithProviders(<IncomesPage />);
    expect(await screen.findByRole('link', { name: 'Gerenciar contas (cotas)' })).toHaveAttribute('href', '/registry');
  });
});

import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
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

  it('RF-28: carries what a third party still owes into the next month and undoes it', async () => {
    let carried: Record<string, unknown> | undefined;
    let undone = false;
    server.use(
      http.get(`${base}/api/occurrences`, () => HttpResponse.json({})),
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: {
            members: [
              { id: 1, code: 'AB', name: 'Casa', active: true, color: '#000' },
              { id: 2, code: 'T', name: 'Tia', active: true, color: '#111' },
              { id: 3, code: 'V', name: 'Vó', active: true, color: '#222' },
            ],
          },
        }),
      ),
      http.get(`${base}/api/summaries/2026-11`, () =>
        HttpResponse.json({
          members: [
            { memberId: 1, main: true, income: 5000, expense: 3000, carriedIn: 0, carriedOut: 0, unpaid: -2000, balance: 2000 },
            { memberId: 2, main: false, income: 100, expense: 300, carriedIn: 50, carriedOut: 0, unpaid: 250, balance: -250 },
            { memberId: 3, main: false, income: 0, expense: 80, carriedIn: 0, carriedOut: 80, unpaid: 80, balance: 0 },
          ],
        }),
      ),
      http.post(`${base}/api/summaries/2026-11/carry-overs`, async ({ request }) => {
        carried = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json([{ memberId: 2, fromPeriod: '2026-11', toPeriod: '2026-12', amount: 250 }]);
      }),
      http.delete(`${base}/api/summaries/2026-11/carry-overs/3`, () => {
        undone = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderWithProviders(<IncomesPage />);
    const table = await screen.findByRole('table', { name: 'Despesa total por conta' });
    expect(await within(table).findByRole('columnheader', { name: 'Saldo anterior' })).toBeInTheDocument();
    expect(within(table).getByRole('columnheader', { name: 'Transportado' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Transportar saldo devido' }));
    const dialog = await screen.findByRole('dialog', { name: 'Transportar saldo devido para dezembro de 2026' });
    expect(within(dialog).getByRole('checkbox', { name: 'T — Tia' })).toBeChecked();
    expect(within(dialog).getByRole('checkbox', { name: 'V — Vó' })).toBeDisabled();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Desfazer transporte de V — Vó' }));
    await waitFor(() => expect(undone).toBe(true));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Transportar' }));
    await waitFor(() => expect(carried).toEqual({ memberIds: [2] }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('RF-28: the carry-over button is off when no third party owes anything', async () => {
    server.use(http.get(`${base}/api/occurrences`, () => HttpResponse.json({})));
    renderWithProviders(<IncomesPage />);
    await screen.findByText('60,0%');
    expect(screen.getByRole('button', { name: 'Transportar saldo devido' })).toBeDisabled();
    expect(screen.queryByRole('columnheader', { name: 'Saldo anterior' })).not.toBeInTheDocument();
  });

  it('RF-20: the members registry is reachable from the incomes tab', async () => {
    server.use(http.get(`${base}/api/occurrences`, () => HttpResponse.json({})));
    renderWithProviders(<IncomesPage />);
    expect(await screen.findByRole('link', { name: 'Gerenciar contas (cotas)' })).toHaveAttribute('href', '/registry');
  });
});

import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { RecurrenceRuleForm } from './RecurrenceRuleForm';

const base = 'http://127.0.0.1:9999';

describe('RecurrenceRuleForm', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({ _embedded: { members: [{ id: 1, code: 'AB', name: 'Casa', active: true }] } }),
      ),
      http.get(`${base}/api/categories`, () => HttpResponse.json({ _embedded: { categories: [{ id: 5, name: 'Moradia' }] } })),
    );
  });

  it('IMP-30, RF-21: one form; incomes ask for the type and are created with kind INCOME', async () => {
    let body: Record<string, unknown> | undefined;
    let kind: string | null = null;
    server.use(
      http.post(`${base}/api/recurrence-rules`, async ({ request }) => {
        kind = new URL(request.url).searchParams.get('kind');
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 1 }, { status: 201 });
      }),
    );
    const done = vi.fn();
    renderWithProviders(<RecurrenceRuleForm kind="INCOME" period="2026-11" onDone={done} />);
    expect(screen.getByLabelText('Tipo')).toBeInTheDocument();
    expect(screen.queryByLabelText('Forma de pagamento')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Descrição'), { target: { value: 'Salário' } });
    await screen.findByRole('option', { name: 'AB — Casa' });
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: '1' } });
    fireEvent.change(screen.getByLabelText('Valor padrão'), { target: { value: '5.000,00' } });
    fireEvent.blur(screen.getByLabelText('Valor padrão'));
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(done).toHaveBeenCalled());
    expect(kind).toBe('INCOME');
    expect(body).toMatchObject({
      description: 'Salário',
      memberId: 1,
      incomeType: 'SALARY',
      defaultAmount: 5000,
      recurrence: 'MONTHLY',
      validFrom: '2026-11',
    });
    expect(body?.paymentMethod).toBeUndefined();
  });

  it('IMP-30, RF-30: fixed expenses ask for category and payment method instead of type', () => {
    renderWithProviders(<RecurrenceRuleForm kind="FIXED_EXPENSE" period="2026-11" onDone={() => undefined} />);
    expect(screen.getByLabelText('Forma de pagamento')).toBeInTheDocument();
    expect(screen.getByLabelText('Categoria')).toBeInTheDocument();
    expect(screen.queryByLabelText('Tipo')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Dia de vencimento')).toHaveValue(10);
  });

  it('RF-23: a one-off income starts as ONCE in the period and hides the end', () => {
    renderWithProviders(<RecurrenceRuleForm kind="INCOME" oneOff period="2026-11" onDone={() => undefined} />);
    expect(screen.getByLabelText('Recorrência')).toHaveValue('ONCE');
    expect(screen.queryByLabelText('Fim (opcional)')).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Recorrência'), { target: { value: 'EVERY_N_MONTHS' } });
    expect(screen.getByLabelText('N (meses)')).toHaveValue(2);
  });
});

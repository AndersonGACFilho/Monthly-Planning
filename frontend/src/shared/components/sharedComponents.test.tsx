import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { useState } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { useNotifications } from '../state/notifications';
import { CategoryIcon } from './CategoryIcon';
import { CategoryPicker } from './CategoryPicker';
import { Field } from './Field';
import { MemberPicker } from './MemberPicker';
import { MoneyText } from './MoneyText';
import { PlaceholderPage } from './PlaceholderPage';
import { ActiveToggle, DeleteButton } from './ResourceActions';
import type { Split } from '../split';
import { SplitEditor } from './SplitEditor';
import { SummaryCard } from './SummaryCard';
import { NOTICE_TIMEOUT, Toaster } from './Toaster';

const base = 'http://127.0.0.1:9999';

describe('shared components', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: {
            members: [
              { id: 1, code: 'AB', name: 'Casa', active: true },
              { id: 2, code: 'X', name: 'Antiga', active: false },
            ],
          },
        }),
      ),
      http.get(`${base}/api/categories`, () =>
        HttpResponse.json({
          _embedded: {
            categories: [
              { id: 1, name: 'Casa' },
              { id: 2, name: 'Luz', parentId: 1 },
            ],
          },
        }),
      ),
    );
  });

  it('IMP-41, I18N-05: MoneyText and SummaryCard show money in the interface language and the alert tone', () => {
    render(<SummaryCard title="Saldo" value={<MoneyText value={-1234.5} />} tone="danger" hint="dica" />);
    expect(screen.getByText('-R$ 1.234,50')).toBeInTheDocument();
    expect(screen.getByText('dica')).toBeInTheDocument();
    expect(screen.getByText('Saldo').closest('section')?.className).toContain('red');
  });

  it('IMP-41: Field links its label and shows the error', () => {
    render(
      <Field label="Nome" htmlFor="name" error="Obrigatório.">
        <input id="name" />
      </Field>,
    );
    expect(screen.getByLabelText('Nome')).toBeInTheDocument();
    expect(screen.getByText('Obrigatório.')).toBeInTheDocument();
  });

  it('RF-01, IMP-41: MemberPicker hides inactive members unless selected; CategoryPicker lists subcategories', async () => {
    const onChange = vi.fn();
    renderWithProviders(
      <>
        <MemberPicker id="m" aria-label="Conta" value={null} allowEmpty emptyLabel="Todas" onChange={onChange} />
        <CategoryPicker id="c" aria-label="Categoria" value={null} allowEmpty onChange={onChange} />
      </>,
    );
    expect(await screen.findByRole('option', { name: 'AB — Casa' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'X — Antiga' })).not.toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'Todas' })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Conta'), { target: { value: '1' } });
    expect(onChange).toHaveBeenCalledWith(1);
    expect(await screen.findByRole('option', { name: /Luz/ })).toBeInTheDocument();
  });

  it('RF-26, IMP-41: SplitEditor splits evenly and warns when shares do not add up to 100%', async () => {
    function Harness() {
      const [value, setValue] = useState<Split>({ mode: 'percentage', shares: [{ memberId: 1, value: 100 }] });
      return <SplitEditor value={value} onChange={setValue} total={100} idPrefix="s" />;
    }
    renderWithProviders(<Harness />);
    fireEvent.click(screen.getByRole('button', { name: /Dividir$/ }));
    const inputs = await screen.findAllByLabelText('Percentual');
    expect(inputs.map((i) => (i as HTMLInputElement).value)).toEqual(['50', '50']);
    fireEvent.change(inputs[0]!, { target: { value: '30' } });
    expect(screen.getByRole('alert')).toHaveTextContent('Faltam 20,0% no rateio');
    fireEvent.click(screen.getAllByRole('button', { name: 'Remover conta do rateio' })[1]!);
    expect(screen.queryByLabelText('Percentual')).not.toBeInTheDocument();
  });

  it('RF-26: SplitEditor switches to amounts, keeping each part, and says what is over', async () => {
    let last: Split | undefined;
    function Harness() {
      const [value, setValue] = useState<Split>({
        mode: 'percentage',
        shares: [{ memberId: 1, value: 30 }, { memberId: 2, value: 70 }],
      });
      last = value;
      return <SplitEditor value={value} onChange={setValue} total={200} idPrefix="s" />;
    }
    renderWithProviders(<Harness />);
    expect(screen.getByRole('button', { name: 'Dividir por percentual' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.click(screen.getByRole('button', { name: 'Dividir por valor' }));
    expect(last).toEqual({ mode: 'amount', shares: [{ memberId: 1, value: 60 }, { memberId: 2, value: 140 }] });
    const amounts = screen.getAllByLabelText('Valor da conta');
    fireEvent.focus(amounts[1]!);
    fireEvent.change(amounts[1]!, { target: { value: '150,00' } });
    expect(screen.getByRole('alert').textContent?.replace(/\u00a0/g, ' ')).toBe('O rateio passa R$ 10,00 do total');
  });

  it('API-09, IMP-41: action buttons appear only with their links and call them', async () => {
    let deleted = false;
    let patched: unknown;
    server.use(
      http.delete(`${base}/api/things/1`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
      http.patch(`${base}/api/things/1`, async ({ request }) => {
        patched = await request.json();
        return HttpResponse.json({});
      }),
    );
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const resource = { version: 3, _links: { delete: { href: `${base}/api/things/1` }, inactivate: { href: `${base}/api/things/1` } } };
    renderWithProviders(
      <>
        <ActiveToggle resource={resource} />
        <DeleteButton resource={resource} />
        <DeleteButton resource={{ _links: {} }} />
      </>,
    );
    expect(screen.getAllByRole('button', { name: 'Excluir' })).toHaveLength(1);
    fireEvent.click(screen.getByRole('button', { name: 'Inativar' }));
    fireEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    await waitFor(() => expect(deleted).toBe(true));
    await waitFor(() => expect(patched).toEqual({ active: false }));
    confirm.mockRestore();
  });

  it('IMP-41: Toaster shows and dismisses notices; placeholders and category icons render', () => {
    render(
      <>
        <Toaster />
        <PlaceholderPage titleKey="nav.templates" />
        <CategoryIcon icon="house" color="#ff0000" />
      </>,
    );
    act(() => useNotifications.getState().push({ kind: 'error', title: 'Falhou', detail: 'disco cheio' }));
    expect(screen.getByText('Falhou')).toBeInTheDocument();
    expect(screen.getByText('disco cheio')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Templates' })).toBeInTheDocument();
  });

  it('IMP-41: notices close by themselves, but not while hovered', () => {
    useNotifications.setState({ notices: [] });
    vi.useFakeTimers();
    try {
      render(<Toaster />);
      act(() => useNotifications.getState().push({ kind: 'success', title: 'Salvo.' }));
      act(() => useNotifications.getState().push({ kind: 'error', title: 'Falhou' }));
      act(() => vi.advanceTimersByTime(NOTICE_TIMEOUT.success));
      expect(screen.queryByText('Salvo.')).not.toBeInTheDocument();
      expect(screen.getByText('Falhou')).toBeInTheDocument();

      fireEvent.pointerEnter(screen.getByRole('alert'));
      act(() => vi.advanceTimersByTime(NOTICE_TIMEOUT.error));
      expect(screen.getByText('Falhou')).toBeInTheDocument();
      fireEvent.pointerLeave(screen.getByRole('alert'));
      act(() => vi.advanceTimersByTime(NOTICE_TIMEOUT.error));
      expect(screen.queryByText('Falhou')).not.toBeInTheDocument();
    } finally {
      vi.useRealTimers();
    }
  });
});

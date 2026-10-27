import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { CategoryPickerWithAdd, MemberPickerWithAdd } from './QuickAddPickers';

const base = 'http://127.0.0.1:9999';

describe('QuickAddPickers', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.get(`${base}/api/members`, () => HttpResponse.json({})),
      http.get(`${base}/api/categories`, () => HttpResponse.json({})),
    );
  });

  it('RF-06, RF-76: a category is created in a modal and selected right away', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.post(`${base}/api/categories`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 42, name: body.name }, { status: 201 });
      }),
    );
    const onChange = vi.fn();
    renderWithProviders(<CategoryPickerWithAdd aria-label="Categoria" allowEmpty value={null} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: 'Nova categoria' }));
    const dialog = await screen.findByRole('dialog', { name: 'Nova categoria' });
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Pets' } });
    fireEvent.submit(dialog.querySelector('form')!);
    await waitFor(() => expect(onChange).toHaveBeenCalledWith(42));
    expect(body).toMatchObject({ name: 'Pets' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('RF-01, RF-76: a member is created in a modal; Escape closes it without saving', async () => {
    const onChange = vi.fn();
    renderWithProviders(<MemberPickerWithAdd aria-label="Conta" allowEmpty value={null} onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: 'Nova conta' }));
    expect(await screen.findByRole('dialog', { name: 'Nova conta' })).toBeInTheDocument();
    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(onChange).not.toHaveBeenCalled();
  });
});

import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { CategoryEditor } from './CategoryEditor';

const base = 'http://127.0.0.1:9999';

describe('CategoryEditor', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    server.use(http.get(`${base}/api/categories`, () => HttpResponse.json({})));
  });

  it('IMP-65: marks a new category as subscription', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.post(`${base}/api/categories`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 9, ...body }, { status: 201 });
      }),
    );
    renderWithProviders(<CategoryEditor onDone={() => undefined} />);
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Streaming' } });
    fireEvent.click(screen.getByRole('checkbox', { name: /Assinatura/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(body).toMatchObject({ name: 'Streaming', subscription: true }));
  });

  it('IMP-65: keeps the mark of an edited category and hides it for the system category', () => {
    const { unmount } = renderWithProviders(
      <CategoryEditor category={{ id: 3, name: 'Assinaturas', subscription: true, system: false }} onDone={() => undefined} />,
    );
    expect(screen.getByRole('checkbox', { name: /Assinatura/ })).toBeChecked();
    unmount();
    renderWithProviders(
      <CategoryEditor category={{ id: 1, name: 'Não classificado', subscription: false, system: true }} onDone={() => undefined} />,
    );
    expect(screen.queryByRole('checkbox', { name: /Assinatura/ })).not.toBeInTheDocument();
  });
});

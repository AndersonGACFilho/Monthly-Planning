import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { RuleRemoval } from './RuleRemoval';

const base = 'http://127.0.0.1:9999';
const self = `${base}/api/recurrence-rules/1`;

describe('RuleRemoval', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });
  afterEach(() => vi.restoreAllMocks());

  it('IMP-63: deletes a rule without history after confirmation', async () => {
    let deleted = false;
    server.use(
      http.delete(self, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<RuleRemoval rule={{ _links: { delete: { href: self } } }} from="2026-11" />);
    fireEvent.click(screen.getByRole('button', { name: 'Excluir' }));
    await waitFor(() => expect(deleted).toBe(true));
    expect(window.confirm).toHaveBeenCalled();
  });

  it('IMP-63: ends a rule with history from the given month', async () => {
    let body: unknown;
    server.use(
      http.post(`${self}/end`, async ({ request }) => {
        body = await request.json();
        return new HttpResponse(null, { status: 204 });
      }),
    );
    renderWithProviders(<RuleRemoval rule={{ _links: { end: { href: `${self}/end` } } }} from="2026-12" />);
    fireEvent.click(screen.getByRole('button', { name: 'Encerrar a partir de dezembro de 2026' }));
    await waitFor(() => expect(body).toEqual({ billingPeriod: '2026-12' }));
  });

  it('IMP-63: does nothing when the confirmation is declined', () => {
    vi.mocked(window.confirm).mockReturnValue(false);
    renderWithProviders(<RuleRemoval rule={{ _links: { end: { href: `${self}/end` } } }} from="2026-12" />);
    fireEvent.click(screen.getByRole('button', { name: 'Encerrar a partir de dezembro de 2026' }));
    expect(window.confirm).toHaveBeenCalledWith(
      'Encerrar a partir de dezembro de 2026? Os meses anteriores ficam como estão e os seguintes ainda não recebidos ou pagos são removidos.',
    );
  });

  it('IMP-63: shows nothing without delete or end', () => {
    const { container } = renderWithProviders(<RuleRemoval rule={{ _links: {} }} from="2026-12" />);
    expect(container).toBeEmptyDOMElement();
  });
});

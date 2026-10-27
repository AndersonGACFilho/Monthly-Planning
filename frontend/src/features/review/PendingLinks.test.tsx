import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { PendingLinks } from './PendingLinks';

const base = 'http://127.0.0.1:9999';

describe('PendingLinks', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('RN-30: a fixed expense with several candidate entries is linked to the one chosen', async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/occurrences/pending-links`, () =>
        HttpResponse.json({
          _embedded: {
            occurrences: [{ id: 4, description: 'Academia', billingPeriod: '2026-11', amount: 99.9, linkCandidates: [10, 11] }],
          },
        }),
      ),
      http.get(`${base}/api/bill-entries/:id`, ({ params }) =>
        HttpResponse.json({ entryId: Number(params.id), entryDate: params.id === '10' ? '2026-10-03' : '2026-10-20', displayDescription: 'ACADEMIA FIT', amount: 99.9 }),
      ),
      http.post(`${base}/api/occurrences/4/link`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ id: 4 });
      }),
    );
    renderWithProviders(<PendingLinks />);
    expect(await screen.findByText('Academia')).toBeInTheDocument();
    await screen.findByRole('option', { name: /20\/10\/2026/ });
    fireEvent.change(screen.getByLabelText('Escolha o lançamento'), { target: { value: '11' } });
    fireEvent.click(screen.getByRole('button', { name: 'Escolha o lançamento' }));
    await waitFor(() => expect(body).toEqual({ entryId: 11 }));
  });
});

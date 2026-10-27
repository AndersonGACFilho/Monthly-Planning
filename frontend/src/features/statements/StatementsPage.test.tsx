import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { StatementsPage } from './StatementsPage';

const base = 'http://127.0.0.1:9999';
const statementBase = `${base}/api/statements/2026-11/members/2`;

describe('StatementsPage', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-11' });
    server.use(
      http.get(`${base}/api/members`, () =>
        HttpResponse.json({
          _embedded: {
            members: [
              { id: 1, code: 'AB', name: 'Casa', active: true, email: 'casa@example.com' },
              { id: 2, code: 'M', name: 'Mãe', active: true, whatsappPhone: '+5551999990000' },
            ],
          },
        }),
      ),
      http.get(`${base}/api/templates`, () => HttpResponse.json({ _embedded: { templates: [{ id: 5, name: 'Padrão', isDefault: true }] } })),
      http.get(`${base}/api/statements/2026-11/members/2`, () =>
        HttpResponse.json({
          memberId: 2,
          memberCode: 'M',
          total: 455.5,
          language: 'pt-BR',
          pendingItems: ['FARMACIA'],
          _links: { preview: { href: `${statementBase}/html` }, pdf: { href: `${statementBase}/pdf` }, png: { href: `${statementBase}/png` } },
        }),
      ),
      http.get(`${statementBase}/html`, () => HttpResponse.text('<p>Extrato da Mãe</p>')),
      http.get(`${base}/api/statement-deliveries`, () =>
        HttpResponse.json({
          _embedded: {
            deliveries: [
              { id: 9, memberId: 2, billingPeriod: '2026-11', channel: 'WHATSAPP', status: 'AWAITING_USER', createdAt: '2026-11-05T12:00:00Z',
                templateId: 5, templateVersion: 1, language: 'pt-BR', outdated: true,
                _links: { resend: { href: `${base}/api/statement-deliveries/9/resend` }, 'mark-sent': { href: `${base}/api/statement-deliveries/9/sent` } } },
            ],
          },
        }),
      ),
    );
  });

  it('RF-81, RF-85, RF-79b: selecting a member shows the preview and the pending warning', async () => {
    renderWithProviders(<StatementsPage />);
    fireEvent.click(await screen.findByRole('checkbox', { name: /M — Mãe/ }));
    expect(await screen.findByText(/1 lançamento desta cota ainda está pendente/)).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTitle('Prévia do extrato')).toHaveAttribute('srcdoc', '<p>Extrato da Mãe</p>'));
  });

  it('RF-79b, RF-91: sending with pending entries asks to confirm and sends anyway; outcomes are listed', async () => {
    const bodies: Record<string, unknown>[] = [];
    server.use(
      http.post(`${base}/api/statement-deliveries`, async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>;
        bodies.push(body);
        if (!body.sendAnyway) {
          return HttpResponse.json({ status: 409, code: 'statement-has-pending', detail: '1 lançamentos pendentes' }, { status: 409 });
        }
        return HttpResponse.json([{ memberId: 2, deliveryId: 10, status: 'SENT', detail: '/data/exports/2026-11/extrato-m.pdf' }]);
      }),
    );
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true);
    renderWithProviders(<StatementsPage />);
    fireEvent.click(await screen.findByRole('checkbox', { name: /M — Mãe/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Salvar na pasta' }));
    await waitFor(() => expect(bodies).toHaveLength(2));
    expect(confirm).toHaveBeenCalled();
    expect(bodies[1]).toMatchObject({ billingPeriod: '2026-11', memberIds: [2], channel: 'FOLDER', sendAnyway: true, includeFuture: true });
    expect(await screen.findByText('Resultado do envio')).toBeInTheDocument();
    expect(screen.getAllByText('Enviado').length).toBeGreaterThan(0);
    confirm.mockRestore();
  });

  it('RF-92, RF-93: history shows outdated deliveries with resend and mark as sent', async () => {
    let marked = false;
    server.use(http.post(`${base}/api/statement-deliveries/9/sent`, () => {
      marked = true;
      return HttpResponse.json({ id: 9, status: 'SENT' });
    }));
    renderWithProviders(<StatementsPage />);
    expect(await screen.findByText('Desatualizado')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reenviar' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Marcar como enviado' }));
    await waitFor(() => expect(marked).toBe(true));
  });
});

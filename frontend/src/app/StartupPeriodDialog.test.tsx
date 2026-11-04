import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n';
import { useBillingPeriodStore } from '../shared/state/billingPeriod';
import { useUiStore } from '../shared/state/ui';
import { renderWithProviders } from '../test/render';
import { server, useMockApi } from '../test/server';
import { StartupPeriodDialog } from './StartupPeriodDialog';

const base = 'http://127.0.0.1:9999';

describe('StartupPeriodDialog', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    vi.useFakeTimers({ now: new Date(2026, 10, 4), toFake: ['Date'] });
    useUiStore.setState({ periodAsked: false });
    useBillingPeriodStore.setState({ period: '2026-11' });
  });
  afterEach(() => vi.useRealTimers());

  it('IMP-56: asks the month on start with the current one highlighted, and opens the month chosen', async () => {
    server.use(http.get(`${base}/api/settings`, () => HttpResponse.json({ 'ask-period-on-start': 'true' })));
    renderWithProviders(<StartupPeriodDialog />);
    const dialog = await screen.findByRole('dialog', { name: 'Qual mês você quer abrir?' });
    expect(dialog).toHaveTextContent('Mês atual');
    expect(screen.getByRole('button', { name: /novembro de 2026/ })).toHaveTextContent('Mês atual');
    fireEvent.click(screen.getByRole('button', { name: 'outubro de 2026' }));
    expect(useBillingPeriodStore.getState().period).toBe('2026-10');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('IMP-56: another month can be typed, and "do not ask" turns the question off', async () => {
    let saved: Record<string, string> | undefined;
    server.use(
      http.get(`${base}/api/settings`, () => HttpResponse.json({ 'ask-period-on-start': 'true' })),
      http.patch(`${base}/api/settings`, async ({ request }) => {
        saved = (await request.json()) as Record<string, string>;
        return HttpResponse.json(saved);
      }),
    );
    renderWithProviders(<StartupPeriodDialog />);
    fireEvent.change(await screen.findByLabelText('Outro mês'), { target: { value: '2025-03' } });
    fireEvent.click(screen.getByRole('checkbox', { name: /Não perguntar ao abrir o app/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Abrir' }));
    expect(useBillingPeriodStore.getState().period).toBe('2025-03');
    await waitFor(() => expect(saved).toEqual({ 'ask-period-on-start': 'false' }));
  });

  it('IMP-56: does not ask when the setting is off or after the first answer', async () => {
    server.use(http.get(`${base}/api/settings`, () => HttpResponse.json({ 'ask-period-on-start': 'false' })));
    renderWithProviders(<StartupPeriodDialog />);
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    useUiStore.setState({ periodAsked: true });
    server.use(http.get(`${base}/api/settings`, () => HttpResponse.json({ 'ask-period-on-start': 'true' })));
    renderWithProviders(<StartupPeriodDialog />);
    await new Promise((r) => setTimeout(r, 50));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

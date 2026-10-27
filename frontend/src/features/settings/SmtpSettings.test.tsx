import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { SmtpSettings } from './SmtpSettings';

const base = 'http://127.0.0.1:9999';

describe('SmtpSettings', () => {
  useMockApi();
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('RF-86: SMTP settings are saved and the password is sent only when typed', async () => {
    let settings: Record<string, unknown> | undefined;
    let password: Record<string, unknown> | undefined;
    server.use(
      http.get(`${base}/api/settings`, () => HttpResponse.json({ 'smtp-host': '', 'smtp-port': '587', 'smtp-starttls': 'true' })),
      http.get(`${base}/api/settings/smtp-password`, () => HttpResponse.json({ configured: true })),
      http.patch(`${base}/api/settings`, async ({ request }) => {
        settings = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(settings);
      }),
      http.put(`${base}/api/settings/smtp-password`, async ({ request }) => {
        password = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({ configured: true });
      }),
    );
    renderWithProviders(<SmtpSettings />);
    expect(await screen.findByText(/Senha salva/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('Servidor'), { target: { value: 'smtp.example.com' } });
    fireEvent.change(screen.getByLabelText('Senha'), { target: { value: 'app-pass' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(password).toEqual({ password: 'app-pass' }));
    expect(settings).toMatchObject({ 'smtp-host': 'smtp.example.com', 'smtp-port': '587' });
  });
});

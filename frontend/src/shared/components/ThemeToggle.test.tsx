import { fireEvent, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { renderWithProviders } from '../../test/render';
import { server, useMockApi } from '../../test/server';
import { applyTheme } from '../theme';
import { ThemeToggle } from './ThemeToggle';

const base = 'http://127.0.0.1:9999';

describe('ThemeToggle', () => {
  useMockApi();
  const saved: unknown[] = [];
  beforeEach(async () => {
    saved.length = 0;
    applyTheme('light');
    await i18n.changeLanguage('pt-BR');
    server.use(
      http.patch(`${base}/api/settings`, async ({ request }) => {
        saved.push(await request.json());
        return HttpResponse.json({});
      }),
    );
  });
  afterEach(() => applyTheme(''));

  it('UI-08: switches between day and night at once and saves the choice', async () => {
    renderWithProviders(<ThemeToggle />);
    fireEvent.click(screen.getByRole('button', { name: 'Mudar para o modo noturno' }));
    expect(document.documentElement.dataset.theme).toBe('dark');
    await waitFor(() => expect(saved).toEqual([{ theme: 'dark' }]));
    fireEvent.click(await screen.findByRole('button', { name: 'Mudar para o modo diurno' }));
    expect(document.documentElement.dataset.theme).toBe('light');
    await waitFor(() => expect(saved).toEqual([{ theme: 'dark' }, { theme: 'light' }]));
  });
});

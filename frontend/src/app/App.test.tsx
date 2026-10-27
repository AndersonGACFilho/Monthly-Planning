import { QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n';
import { server, useMockApi } from '../test/server';
import { createQueryClient } from './queryClient';
import { routes } from './router';
import { resetStartup } from './useStartup';

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
}

describe('App shell', () => {
  useMockApi();
  beforeEach(() => {
    resetStartup();
    server.use(
      http.get('*/api/settings', () => HttpResponse.json({ language: '' })),
      http.post('*/api/categories/defaults', () => HttpResponse.json({})),
      http.get('*/api/review-queue/summary', () => HttpResponse.json({ count: 3, total: 10 })),
      http.get('*/api/*', () => HttpResponse.json({})),
    );
  });

  it('RNF-10: opens straight on the overview, without login', async () => {
    await i18n.changeLanguage('pt-BR');
    renderAt('/');
    expect(await screen.findByRole('heading', { name: 'Visão geral' })).toBeInTheDocument();
  });

  it('IMP-05: sidebar lists the five tabs in order, then the extra screens', async () => {
    await i18n.changeLanguage('en-US');
    renderAt('/entries');
    const nav = await screen.findByRole('navigation', { name: 'Main menu' });
    const labels = within(nav).getAllByRole('link').map((link) => link.getAttribute('title'));
    expect(labels).toEqual([
      'Overview',
      'Incomes and members',
      'Expenses',
      'Card entries',
      'Import',
      'Review queue',
      'Templates',
      'Deliveries',
      'Settings',
    ]);
  });

  it('RF-79: the review queue entry shows the number of pending items', async () => {
    await i18n.changeLanguage('en-US');
    renderAt('/');
    expect(await screen.findByLabelText('3 pending')).toHaveTextContent('3');
  });
});

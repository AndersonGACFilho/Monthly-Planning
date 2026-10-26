import { render, screen, within } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n/i18n';
import { routes } from './router';

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  return render(<RouterProvider router={router} />);
}

describe('App shell', () => {
  it('RNF-10: opens straight on the overview, without login', async () => {
    await i18n.changeLanguage('pt-BR');
    renderAt('/');
    expect(await screen.findByRole('heading', { name: 'Visão geral' })).toBeInTheDocument();
  });

  it('IMP-05: sidebar lists the five tabs in order, then the extra screens', async () => {
    await i18n.changeLanguage('en-US');
    renderAt('/entries');
    const nav = screen.getByRole('navigation', { name: 'Main menu' });
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
    expect(screen.getByRole('heading', { name: 'Card entries' })).toBeInTheDocument();
  });
});

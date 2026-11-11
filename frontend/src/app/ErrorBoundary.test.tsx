import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../i18n/i18n';
import { useNotifications } from '../shared/state/notifications';
import { ErrorBoundary, RouteError } from './ErrorBoundary';
import { installGlobalErrorHandlers } from './globalErrors';

function Broken(): never {
  throw new Error('render failed');
}

describe('interface errors', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useNotifications.setState({ notices: [] });
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
  });
  afterEach(() => vi.restoreAllMocks());

  it('RNF-11: a screen that fails to render shows a way back and reports the error', () => {
    render(
      <ErrorBoundary>
        <Broken />
      </ErrorBoundary>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('Esta tela falhou');
    expect(screen.getByRole('button', { name: 'Recarregar' })).toBeInTheDocument();
    expect(useNotifications.getState().notices).toHaveLength(1);
  });

  it('RNF-11: a screen of the router that fails is reported and is shown in its place', async () => {
    const router = createMemoryRouter([
      { path: '/', element: <p>menu</p>, children: [] },
      { path: '/broken', element: <Broken />, errorElement: <RouteError /> },
    ], { initialEntries: ['/broken'] });
    render(<RouterProvider router={router} />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Esta tela falhou');
    expect(useNotifications.getState().notices).toHaveLength(1);
  });

  it('RNF-11: unhandled errors and rejections are reported', () => {
    const target = new EventTarget();
    installGlobalErrorHandlers(target);
    target.dispatchEvent(Object.assign(new Event('error'), { error: new Error('boom') }));
    target.dispatchEvent(Object.assign(new Event('unhandledrejection'), { reason: new Error('rejected') }));
    expect(useNotifications.getState().notices).toHaveLength(2);
  });
});

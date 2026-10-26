import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import * as backendConfig from '../api/backendConfig';
import i18n from '../i18n/i18n';
import { BackendGate } from './BackendGate';

describe('BackendGate', () => {
  afterEach(() => vi.restoreAllMocks());

  it('POR-08: shows the loading screen until the service answers, then the app', async () => {
    await i18n.changeLanguage('pt-BR');
    let resolve: (value: backendConfig.BackendConfig) => void = () => {};
    vi.spyOn(backendConfig, 'getBackendConfig').mockReturnValue(new Promise((r) => (resolve = r)));
    render(<BackendGate>app</BackendGate>);
    expect(screen.getByRole('status')).toHaveTextContent('Carregando…');
    resolve({ baseUrl: 'http://127.0.0.1:1', token: 't' });
    expect(await screen.findByText('app')).toBeInTheDocument();
  });

  it('POR-08: shows an error when the service cannot start', async () => {
    await i18n.changeLanguage('en-US');
    vi.spyOn(backendConfig, 'getBackendConfig').mockRejectedValue(new Error('locked'));
    render(<BackendGate>app</BackendGate>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the local service.');
  });
});

import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import * as backendConfig from '../api/backendConfig';
import i18n from '../i18n/i18n';
import { BackendGate, loadingStep } from './BackendGate';

describe('BackendGate', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('POR-08, IMP-57: shows the loading screen with what the service is doing until it answers, then the app', async () => {
    await i18n.changeLanguage('pt-BR');
    vi.useFakeTimers();
    let resolve: (value: backendConfig.BackendConfig) => void = () => {};
    vi.spyOn(backendConfig, 'getBackendConfig').mockReturnValue(new Promise((r) => (resolve = r)));
    render(<BackendGate>app</BackendGate>);
    expect(screen.getByRole('status')).toHaveTextContent('Iniciando o serviço local…');
    expect(screen.getByRole('heading', { name: 'Planejamento Mensal' })).toBeInTheDocument();
    act(() => vi.advanceTimersByTime(7000));
    expect(screen.getByRole('status')).toHaveTextContent('Fazendo o backup de início e conferindo o banco…');
    vi.useRealTimers();
    resolve({ baseUrl: 'http://127.0.0.1:1', token: 't' });
    expect(await screen.findByText('app')).toBeInTheDocument();
  });

  it('POR-08, IMP-57: shows what to check when the service cannot start, and tries again', async () => {
    await i18n.changeLanguage('en-US');
    const config = vi.spyOn(backendConfig, 'getBackendConfig').mockRejectedValueOnce(new Error('locked'));
    render(<BackendGate>app</BackendGate>);
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not reach the local service.');
    expect(screen.getByRole('alert')).toHaveTextContent('Check that the drive is connected');
    config.mockResolvedValueOnce({ baseUrl: 'http://127.0.0.1:1', token: 't' });
    fireEvent.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText('app')).toBeInTheDocument();
    expect(config).toHaveBeenCalledTimes(2);
  });

  it('IMP-57: the steps follow the time waited', () => {
    expect([0, 1, 2, 5, 6, 14, 15, 60].map(loadingStep)).toEqual([
      'starting', 'starting', 'opening', 'opening', 'checking', 'checking', 'slow', 'slow',
    ]);
  });
});

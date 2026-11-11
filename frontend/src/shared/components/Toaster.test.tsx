import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { useNotifications } from '../state/notifications';
import { NOTICE_TIMEOUT, Toaster } from './Toaster';

describe('Toaster', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useNotifications.setState({ notices: [] });
  });
  afterEach(() => vi.useRealTimers());

  it('RNF-11: technical details of an error are shown and copied', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    render(<Toaster />);
    act(() => useNotifications.getState().push({ kind: 'error', title: 'Erro inesperado', details: 'code: internal-error\nrequest: abc' }));
    expect(screen.getByText(/request: abc/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Copiar detalhes' }));
    expect(writeText).toHaveBeenCalledWith('Erro inesperado\ncode: internal-error\nrequest: abc');
  });

  it('RNF-11: a sticky notice stays until closed', () => {
    vi.useFakeTimers();
    render(<Toaster />);
    act(() => useNotifications.getState().push({ kind: 'error', title: 'Falhou', sticky: true }));
    act(() => vi.advanceTimersByTime(NOTICE_TIMEOUT.error * 3));
    expect(screen.getByText('Falhou')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));
    expect(screen.queryByText('Falhou')).not.toBeInTheDocument();
  });
});

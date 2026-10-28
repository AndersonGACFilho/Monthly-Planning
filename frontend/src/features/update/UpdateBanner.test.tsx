import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { UpdateBanner } from './UpdateBanner';
import { useUpdateStore } from './updateStore';
import { checkForUpdate, downloadUpdate, restartToUpdate } from './updates';

vi.mock('./updates', () => ({
  checkForUpdate: vi.fn(),
  downloadUpdate: vi.fn(),
  restartToUpdate: vi.fn(),
  installedVersion: vi.fn(),
}));

const release = { version: '0.2.0', notes: 'v0.2.0\n- Importação mais rápida' };

describe('UpdateBanner', () => {
  beforeEach(async () => {
    vi.resetAllMocks();
    useUpdateStore.setState({ phase: 'idle', info: undefined, progress: undefined, error: undefined, dismissed: false });
    await i18n.changeLanguage('pt-BR');
  });

  it('POR-16: offers a newer version with its notes, downloads it with progress and restarts on request', async () => {
    vi.mocked(checkForUpdate).mockResolvedValue(release);
    let finish = () => {};
    vi.mocked(downloadUpdate).mockImplementation(
      (onProgress) =>
        new Promise((resolve) => {
          onProgress({ downloaded: 50, total: 200 });
          finish = () => resolve();
        }),
    );
    render(<UpdateBanner />);

    expect(await screen.findByText('Nova versão 0.2.0 disponível.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Novidades' }));
    expect(screen.getByRole('dialog', { name: 'Novidades da versão 0.2.0' })).toHaveTextContent('Importação mais rápida');
    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }));

    fireEvent.click(screen.getByRole('button', { name: 'Atualizar' }));
    expect(await screen.findByText('Baixando a versão 0.2.0… 25%')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Depois' })).not.toBeInTheDocument();
    finish();

    fireEvent.click(await screen.findByRole('button', { name: 'Reiniciar agora' }));
    expect(restartToUpdate).toHaveBeenCalledOnce();
  });

  it('POR-16: never forced, the notice can be left for later', async () => {
    vi.mocked(checkForUpdate).mockResolvedValue(release);
    render(<UpdateBanner />);
    fireEvent.click(await screen.findByRole('button', { name: 'Depois' }));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(downloadUpdate).not.toHaveBeenCalled();
  });

  it('POR-16: offline or up to date shows nothing', async () => {
    vi.mocked(checkForUpdate).mockRejectedValue(new Error('network: offline'));
    const { unmount } = render(<UpdateBanner />);
    await waitFor(() => expect(useUpdateStore.getState().phase).toBe('checkFailed'));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    unmount();

    useUpdateStore.setState({ phase: 'idle' });
    vi.mocked(checkForUpdate).mockResolvedValue(null);
    render(<UpdateBanner />);
    await waitFor(() => expect(useUpdateStore.getState().phase).toBe('upToDate'));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('POR-16: a failed download can be tried again', async () => {
    vi.mocked(checkForUpdate).mockResolvedValue(release);
    vi.mocked(downloadUpdate).mockRejectedValueOnce('integrity: SHA-256 differs').mockResolvedValueOnce();
    render(<UpdateBanner />);
    fireEvent.click(await screen.findByRole('button', { name: 'Atualizar' }));
    expect(await screen.findByText('A atualização não foi baixada: integrity: SHA-256 differs')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }));
    expect(await screen.findByRole('button', { name: 'Reiniciar agora' })).toBeInTheDocument();
  });
});

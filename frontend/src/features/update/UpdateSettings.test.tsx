import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { UpdateSettings } from './UpdateSettings';
import { useUpdateStore } from './updateStore';
import { checkForUpdate, downloadUpdate, installedVersion } from './updates';

const desktop = vi.hoisted(() => ({ value: true }));

vi.mock('@tauri-apps/api/core', () => ({ isTauri: () => desktop.value }));
vi.mock('./updates', () => ({
  checkForUpdate: vi.fn(),
  downloadUpdate: vi.fn(),
  restartToUpdate: vi.fn(),
  installedVersion: vi.fn(),
}));

describe('UpdateSettings', () => {
  beforeEach(async () => {
    vi.resetAllMocks();
    desktop.value = true;
    vi.mocked(installedVersion).mockResolvedValue('0.1.0');
    useUpdateStore.setState({ phase: 'idle', info: undefined, progress: undefined, error: undefined, dismissed: false });
    await i18n.changeLanguage('pt-BR');
  });

  it('POR-16: shows the installed version and checks on request', async () => {
    vi.mocked(checkForUpdate).mockResolvedValue(null);
    render(<UpdateSettings />);
    expect(await screen.findByText('Versão instalada: 0.1.0')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Procurar atualizações' }));
    expect(await screen.findByText('O app já está na versão mais recente.')).toBeInTheDocument();
  });

  it('POR-16: a newer version found here can be installed', async () => {
    vi.mocked(checkForUpdate).mockResolvedValue({ version: '0.2.0', notes: '' });
    vi.mocked(downloadUpdate).mockResolvedValue();
    render(<UpdateSettings />);
    fireEvent.click(screen.getByRole('button', { name: 'Procurar atualizações' }));
    fireEvent.click(await screen.findByRole('button', { name: 'Atualizar para a versão 0.2.0' }));
    expect(await screen.findByText(/A versão 0.2.0 está pronta/)).toBeInTheDocument();
  });

  it('POR-16: tells when GitHub cannot be reached', async () => {
    vi.mocked(checkForUpdate).mockRejectedValue(new Error('offline'));
    render(<UpdateSettings />);
    fireEvent.click(screen.getByRole('button', { name: 'Procurar atualizações' }));
    expect(await screen.findByText(/Não foi possível falar com o GitHub/)).toBeInTheDocument();
  });

  it('POR-16: the browser preview has nothing to update', () => {
    desktop.value = false;
    const { container } = render(<UpdateSettings />);
    expect(container).toBeEmptyDOMElement();
  });
});

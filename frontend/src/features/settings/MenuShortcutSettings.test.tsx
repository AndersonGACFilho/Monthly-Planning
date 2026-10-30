import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { useNotifications } from '../../shared/state/notifications';
import { MenuShortcutSettings } from './MenuShortcutSettings';
import { addShortcut, removeShortcut, shortcutStatus } from './menuShortcut';

const desktop = vi.hoisted(() => ({ value: true }));

vi.mock('@tauri-apps/api/core', () => ({ isTauri: () => desktop.value }));
vi.mock('./menuShortcut', () => ({ shortcutStatus: vi.fn(), addShortcut: vi.fn(), removeShortcut: vi.fn() }));

const add = 'Adicionar ao menu do sistema';
const remove = 'Remover do menu do sistema';
const fix = 'Corrigir atalho';

describe('MenuShortcutSettings', () => {
  beforeEach(async () => {
    vi.resetAllMocks();
    desktop.value = true;
    useNotifications.setState({ notices: [] });
    await i18n.changeLanguage('pt-BR');
  });

  it('POR-18: offers to add the app and never adds it by itself', async () => {
    vi.mocked(shortcutStatus).mockResolvedValue({ state: 'absent' });
    render(<MenuShortcutSettings />);
    expect(await screen.findByText(/ainda não encontra o app/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: add })).toBeEnabled();
    expect(screen.queryByRole('button', { name: remove })).not.toBeInTheDocument();
    expect(addShortcut).not.toHaveBeenCalled();
  });

  it('POR-18: adding shows the app as found, and removing takes it out', async () => {
    vi.mocked(shortcutStatus).mockResolvedValueOnce({ state: 'absent' }).mockResolvedValueOnce({ state: 'current' });
    vi.mocked(addShortcut).mockResolvedValue();
    render(<MenuShortcutSettings />);
    fireEvent.click(await screen.findByRole('button', { name: add }));
    expect(await screen.findByText(/O app aparece na busca/)).toBeInTheDocument();
    expect(addShortcut).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: add })).not.toBeInTheDocument();

    vi.mocked(shortcutStatus).mockResolvedValueOnce({ state: 'absent' });
    vi.mocked(removeShortcut).mockResolvedValue();
    fireEvent.click(screen.getByRole('button', { name: remove }));
    expect(await screen.findByRole('button', { name: add })).toBeInTheDocument();
    expect(removeShortcut).toHaveBeenCalledTimes(1);
  });

  it('POR-18: a shortcut to another drive letter or mount path is offered a fix', async () => {
    vi.mocked(shortcutStatus)
      .mockResolvedValueOnce({ state: 'elsewhere', folder: 'E:\\PlanejamentoMensal' })
      .mockResolvedValueOnce({ state: 'current' });
    vi.mocked(addShortcut).mockResolvedValue();
    render(<MenuShortcutSettings />);
    expect(await screen.findByText(/abre o app em E:\\PlanejamentoMensal/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: remove })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: fix }));
    expect(await screen.findByText(/O app aparece na busca/)).toBeInTheDocument();
    expect(addShortcut).toHaveBeenCalledTimes(1);
  });

  it('POR-18: a failure is reported and the state read again', async () => {
    vi.mocked(shortcutStatus).mockResolvedValue({ state: 'absent' });
    vi.mocked(addShortcut).mockRejectedValue(new Error('io: permission denied'));
    render(<MenuShortcutSettings />);
    fireEvent.click(await screen.findByRole('button', { name: add }));
    await vi.waitFor(() => expect(useNotifications.getState().notices).not.toHaveLength(0));
    expect(shortcutStatus).toHaveBeenCalledTimes(2);
    expect(await screen.findByRole('button', { name: add })).toBeEnabled();
  });

  it('POR-18: hidden where the system has no menu the app can use', async () => {
    vi.mocked(shortcutStatus).mockRejectedValue(new Error('this system has no menu to add the app to'));
    const { container } = render(<MenuShortcutSettings />);
    await vi.waitFor(() => expect(shortcutStatus).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });

  it('POR-18: the browser preview has no system menu', () => {
    desktop.value = false;
    const { container } = render(<MenuShortcutSettings />);
    expect(container).toBeEmptyDOMElement();
    expect(shortcutStatus).not.toHaveBeenCalled();
  });
});

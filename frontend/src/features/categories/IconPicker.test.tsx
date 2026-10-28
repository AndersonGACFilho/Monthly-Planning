import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { Modal } from '../../shared/components/Modal';
import { IconPicker } from './IconPicker';

describe('IconPicker', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
  });

  it('RF-06: the grid shows previews named on hover, the search narrows it and a pick closes it', () => {
    const onChange = vi.fn();
    render(<IconPicker id="icon" value="house" color="#123456" onChange={onChange} />);
    fireEvent.click(screen.getByRole('button', { name: 'Escolher ícone (atual: house)' }));

    expect(screen.getByRole('button', { name: 'house' })).toHaveAttribute('aria-pressed', 'true');
    fireEvent.change(screen.getByRole('searchbox', { name: 'Buscar ícone' }), { target: { value: 'piggy' } });
    expect(screen.queryByRole('button', { name: 'house' })).not.toBeInTheDocument();
    const piggy = screen.getByRole('button', { name: 'piggy-bank' });
    expect(piggy).toHaveAttribute('title', 'piggy-bank');

    fireEvent.click(piggy);
    expect(onChange).toHaveBeenCalledWith('piggy-bank');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('RF-06: a search with no match says so', () => {
    render(<IconPicker id="icon" value="house" color="#123456" onChange={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: /Escolher ícone/ }));
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'zzzz' } });
    expect(screen.getByText('Nenhum ícone encontrado.')).toBeInTheDocument();
  });

  it('RF-76: inside another modal, Escape closes only the icon grid', async () => {
    const onOuterClose = vi.fn();
    render(
      <Modal title="Nova categoria" onClose={onOuterClose}>
        <IconPicker id="icon" value="house" color="#123456" onChange={vi.fn()} />
      </Modal>,
    );
    fireEvent.click(screen.getByRole('button', { name: /Escolher ícone/ }));
    expect(screen.getByRole('dialog', { name: 'Escolher ícone' })).toBeInTheDocument();

    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByRole('dialog', { name: 'Escolher ícone' })).not.toBeInTheDocument());
    expect(onOuterClose).not.toHaveBeenCalled();
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(onOuterClose).toHaveBeenCalledTimes(1);
  });
});

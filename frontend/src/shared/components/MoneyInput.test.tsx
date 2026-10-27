import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { MoneyInput } from './MoneyInput';

describe('MoneyInput', () => {
  it('I18N-05: shows and parses amounts in pt-BR', async () => {
    await i18n.changeLanguage('pt-BR');
    const onChange = vi.fn();
    render(<MoneyInput value={1234.5} onChange={onChange} aria-label="valor" />);
    const input = screen.getByLabelText('valor');
    expect(input).toHaveValue('1.234,50');
    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: '89,90' } });
    expect(onChange).toHaveBeenLastCalledWith(89.9);
    fireEvent.change(input, { target: { value: '-1,00' } });
    expect(onChange).toHaveBeenLastCalledWith(null);
  });

  it('I18N-05: accepts negative amounts when allowed, in en-US', async () => {
    await i18n.changeLanguage('en-US');
    const onChange = vi.fn();
    render(<MoneyInput value={null} onChange={onChange} allowNegative aria-label="amount" />);
    fireEvent.change(screen.getByLabelText('amount'), { target: { value: '-1,000.25' } });
    expect(onChange).toHaveBeenLastCalledWith(-1000.25);
  });
});

import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../state/billingPeriod';
import { BillingPeriodSelector, relativeMonths } from './BillingPeriodSelector';

describe('BillingPeriodSelector', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-12' });
  });
  afterEach(() => vi.useRealTimers());

  it('IMP-55: the current month stands out; another month says how far it is and offers the way back', () => {
    vi.useFakeTimers({ now: new Date(2026, 11, 10), toFake: ['Date'] });
    render(<BillingPeriodSelector />);
    expect(screen.getByText('Mês atual')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Ir para o mês atual/ })).not.toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('Mês anterior'));
    expect(screen.getByText('mês passado')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /Ir para o mês atual/ }));
    expect(useBillingPeriodStore.getState().period).toBe('2026-12');
  });

  it('IMP-55: distance between months in words', () => {
    expect(relativeMonths('2026-12', '2027-02', 'pt-BR')).toBe('em 2 meses');
    expect(relativeMonths('2026-12', '2026-09', 'en-US')).toBe('3 months ago');
  });

  it('UI-02: shows the month and moves with the buttons', () => {
    render(<BillingPeriodSelector />);
    expect(screen.getByText('dezembro de 2026')).toBeInTheDocument();
    fireEvent.click(screen.getByLabelText('Próximo mês'));
    expect(useBillingPeriodStore.getState().period).toBe('2027-01');
    fireEvent.click(screen.getByLabelText('Mês anterior'));
    fireEvent.click(screen.getByLabelText('Mês anterior'));
    expect(useBillingPeriodStore.getState().period).toBe('2026-11');
  });

  it('UI-01: Alt+← and Alt+→ change the billing period from anywhere', () => {
    render(<BillingPeriodSelector />);
    fireEvent.keyDown(window, { key: 'ArrowRight', altKey: true });
    expect(useBillingPeriodStore.getState().period).toBe('2027-01');
    fireEvent.keyDown(window, { key: 'ArrowLeft', altKey: true });
    fireEvent.keyDown(window, { key: 'ArrowLeft' });
    expect(useBillingPeriodStore.getState().period).toBe('2026-12');
  });
});

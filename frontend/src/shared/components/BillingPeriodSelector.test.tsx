import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { useBillingPeriodStore } from '../state/billingPeriod';
import { BillingPeriodSelector } from './BillingPeriodSelector';

describe('BillingPeriodSelector', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('pt-BR');
    useBillingPeriodStore.setState({ period: '2026-12' });
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

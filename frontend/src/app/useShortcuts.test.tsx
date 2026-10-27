import { QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router';
import { beforeEach, describe, expect, it } from 'vitest';
import { useBillingPeriodStore } from '../shared/state/billingPeriod';
import { useUiStore } from '../shared/state/ui';
import { BillingPeriodSelector } from '../shared/components/BillingPeriodSelector';
import { createQueryClient } from './queryClient';
import { useShortcuts } from './useShortcuts';

function Probe() {
  useShortcuts();
  const location = useLocation();
  return (
    <main>
      <p data-testid="where">{location.pathname + location.search}</p>
      <input type="search" aria-label="busca" />
      <BillingPeriodSelector />
    </main>
  );
}

describe('useShortcuts', () => {
  beforeEach(() => {
    useBillingPeriodStore.setState({ period: '2026-11' });
    useUiStore.setState({ exportOpen: false });
  });

  it('UI-01: Ctrl+N, Ctrl+I, Ctrl+E, Ctrl+F and Alt+arrows reach their actions', () => {
    render(
      <QueryClientProvider client={createQueryClient()}>
        <MemoryRouter>
          <Routes>
            <Route path="*" element={<Probe />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    fireEvent.keyDown(window, { key: 'n', ctrlKey: true });
    expect(screen.getByTestId('where')).toHaveTextContent('/entries?new=1');
    fireEvent.keyDown(window, { key: 'i', ctrlKey: true });
    expect(screen.getByTestId('where')).toHaveTextContent('/import');
    fireEvent.keyDown(window, { key: 'e', ctrlKey: true });
    expect(useUiStore.getState().exportOpen).toBe(true);
    fireEvent.keyDown(window, { key: 'f', ctrlKey: true });
    expect(document.activeElement).toBe(screen.getByLabelText('busca'));
    fireEvent.keyDown(window, { key: 'ArrowRight', altKey: true });
    expect(useBillingPeriodStore.getState().period).toBe('2026-12');
    fireEvent.keyDown(window, { key: 'ArrowLeft', altKey: true });
    fireEvent.keyDown(window, { key: 'ArrowLeft', altKey: true });
    expect(useBillingPeriodStore.getState().period).toBe('2026-10');
  });
});

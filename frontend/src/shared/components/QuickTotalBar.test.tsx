import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import i18n from '../../i18n/i18n';
import { QuickTotalBar } from './QuickTotalBar';

describe('QuickTotalBar', () => {
  it('RF-42, RNF-01: sums filtered and selected amounts in cents', async () => {
    await i18n.changeLanguage('en-US');
    render(<QuickTotalBar filtered={[0.1, 0.2, '48.01']} selected={[]} />);
    const bar = screen.getByRole('status');
    expect(bar).toHaveTextContent('3 filtered');
    expect(bar).toHaveTextContent('R$48.31');
    expect(bar).toHaveTextContent('0 selected');
  });
});

import { iconNames } from 'lucide-react/dynamic';
import { describe, expect, it } from 'vitest';
import { SUGGESTED_ICONS, searchIcons } from './iconSearch';

describe('searchIcons', () => {
  it('RF-06: with no query, the suggested icons come first and the rest follow', () => {
    const { icons, total } = searchIcons('  ', 10);
    expect(icons).toEqual(SUGGESTED_ICONS.slice(0, 10));
    expect(total).toBe(iconNames.length);
  });

  it('RF-06: every word of the query must be in the name', () => {
    const { icons } = searchIcons('Shopping cart', 50);
    expect(icons).toContain('shopping-cart');
    expect(icons.every((name) => name.includes('shopping') && name.includes('cart'))).toBe(true);
  });

  it('RF-06: the limit keeps the grid small but the total counts every match', () => {
    const { icons, total } = searchIcons('arrow', 5);
    expect(icons).toHaveLength(5);
    expect(total).toBeGreaterThan(5);
    expect(searchIcons('no-such-icon-anywhere', 5)).toEqual({ icons: [], total: 0 });
  });
});

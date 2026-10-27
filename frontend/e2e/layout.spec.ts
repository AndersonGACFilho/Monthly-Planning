import { expect, test } from '@playwright/test';
import { isWide, texts } from './support';

test('RNF-06, IMP-45: the sidebar collapses to icons below 1200 px and shows labels above', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/');
  const label = page.locator('nav').getByText(t('nav.entries'), { exact: true });
  await expect(page.getByRole('link', { name: t('nav.entries') })).toBeVisible();
  // Below 1200 px the label is only for screen readers (sr-only: absolutely positioned, 1 px)
  await expect(label).toHaveCSS('position', isWide(info) ? 'static' : 'absolute');
  const width = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(width).toBeLessThanOrEqual(0);
});

test('IMP-45, UI-04: the entry details panel only opens on wide windows', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/entries');
  const firstRow = page.locator('main tbody tr').first();
  await expect(firstRow.locator('td button').first()).toBeVisible();
  await firstRow.locator('td').first().click({ position: { x: 4, y: 4 } });
  const details = page.getByRole('complementary', { name: t('entries.details') });
  if (isWide(info)) {
    await expect(details).toBeVisible();
  } else {
    await expect(details).toBeHidden();
  }
});

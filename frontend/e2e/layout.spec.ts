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

for (const path of ['/deliveries', '/entries', '/review-queue', '/registry']) {
  test(`RNF-06, IMP-45: ${path} scrolls only the content; the menu reaches the bottom of the window`, async ({ page }) => {
    await page.goto(path);
    await page.waitForLoadState('networkidle');
    const box = await page.evaluate(() => {
      window.scrollTo(0, 1000);
      return {
        scrolled: window.scrollY,
        overflow: document.documentElement.scrollHeight - document.documentElement.clientHeight,
        navBottom: document.querySelector('nav')!.getBoundingClientRect().bottom,
        height: window.innerHeight,
      };
    });
    expect(box.scrolled).toBe(0);
    expect(box.overflow).toBeLessThanOrEqual(0);
    expect(Math.round(box.navBottom)).toBe(box.height);
  });
}

test('RF-76, IMP-45: the validate button of the review queue is never cut at the right edge', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/review-queue');
  const button = page.locator('main tbody tr').first().getByRole('button', { name: t('review.validate'), exact: true });
  await expect(button).toBeVisible();
  const right = await button.evaluate((el) => el.getBoundingClientRect().right);
  const mainRight = await page.locator('main').evaluate((el) => el.getBoundingClientRect().right);
  expect(right).toBeLessThanOrEqual(mainRight);
});

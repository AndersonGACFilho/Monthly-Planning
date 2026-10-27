import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import { texts } from './support';

for (const path of ['/', '/incomes', '/expenses', '/entries', '/import', '/review-queue', '/deliveries', '/templates', '/registry', '/settings']) {
  test(`IMP-48: ${path} has no serious accessibility violations`, async ({ page }) => {
    await page.goto(path);
    await page.waitForLoadState('networkidle');
    const results = await new AxeBuilder({ page }).include('main').disableRules(['color-contrast']).analyze();
    const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');
    expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(' ')).slice(0, 3).join(', ')}`)).toEqual([]);
  });
}

test('IMP-48, UI-05: the review queue works from the keyboard alone', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/review-queue');
  const first = page.locator('main tbody tr').first();
  await expect(first).toBeVisible();
  await first.getByLabel(t('review.category'), { exact: true }).focus();
  await expect(first.getByLabel(t('review.category'), { exact: true })).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.locator(':focus')).toBeVisible();
});

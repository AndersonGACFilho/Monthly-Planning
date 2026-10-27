import { expect, test } from '@playwright/test';
import { texts } from './support';

test('UI-01: keyboard shortcuts open import, new entry, export and change the period', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/');
  await expect(page.getByRole('heading', { name: t('nav.overview') })).toBeVisible();
  await page.locator('main').click({ position: { x: 5, y: 5 } });
  await page.keyboard.press('Control+i');
  await expect(page).toHaveURL(/\/import$/);
  await page.keyboard.press('Control+n');
  await expect(page).toHaveURL(/\/entries/);
  await expect(page.getByRole('form', { name: t('entries.manual') }).or(page.getByText(t('entries.manual')).first())).toBeVisible();
  await page.keyboard.press('Control+e');
  await expect(page.getByRole('dialog', { name: t('workbook.title') })).toBeVisible();
  const group = page.getByRole('group', { name: t('period.label') });
  const before = await group.textContent();
  await page.keyboard.press('Alt+ArrowRight');
  await expect(group).not.toHaveText(before ?? '');
});

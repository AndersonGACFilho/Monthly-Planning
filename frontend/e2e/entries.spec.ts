import { expect, test } from '@playwright/test';
import { texts } from './support';

test('RF-40, RF-46, RF-50, IMP-40: an entry is renamed inline and restored', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/entries');
  const first = page.locator('main tbody tr').first().locator('td button').first();
  await expect(first).toBeVisible();
  const original = (await first.textContent())?.trim() ?? '';
  await first.click();
  const input = page.getByLabel(t('entries.alias'));
  await input.fill('Nome renomeado E2E');
  await page.getByRole('button', { name: t('action.save') }).first().click();
  await expect(page.getByText('Nome renomeado E2E').first()).toBeVisible();
  await page.getByRole('button', { name: t('entries.restoreOriginal') }).first().click();
  await expect(page.getByText(original).first()).toBeVisible();
});

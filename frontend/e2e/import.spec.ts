import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';
import { texts } from './support';

const PDF = fileURLToPath(new URL('../../backend/src/test/resources/fixtures/itau/itau-closed-bill-2026-10.pdf', import.meta.url));

test('RF-60, RF-68, IMP-40: a bill shows a preview with totals and can be discarded', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/import');
  await page.getByLabel(t('period.label'), { exact: true }).last().fill('2026-10');
  await page.locator('input[type=file]').setInputFiles(PDF);
  await expect(page.getByRole('button', { name: t('import.confirm') })).toBeVisible();
  await expect(page.getByText(t('import.fileTotal'))).toBeVisible();
  await page.getByRole('button', { name: t('import.cancel') }).click();
  await expect(page.getByRole('button', { name: t('import.confirm') })).toHaveCount(0);
});

import { expect, test } from '@playwright/test';
import { texts } from './support';

test('RF-81, RF-84, RF-85, IMP-40: a member statement is previewed and downloaded as PDF', async ({ page }, info) => {
  const t = texts(info);
  await page.goto('/deliveries');
  await page.getByRole('checkbox', { name: /AB —/ }).check();
  const frame = page.getByTitle(t('statements.previewFrame'));
  await expect(frame).toHaveAttribute('srcdoc', /Itaú Platinum/);
  const download = page.waitForEvent('download');
  await page.getByRole('button', { name: 'PDF' }).click();
  expect((await download).suggestedFilename()).toMatch(/-ab-\d{4}-\d{2}\.pdf$/);
});

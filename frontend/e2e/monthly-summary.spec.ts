import { expect, test } from '@playwright/test';
import { api, texts } from './support';

test('RF-10, RF-11, IMP-33, IMP-40: the overview shows the totals of the API summary', async ({ page }, info) => {
  const t = texts(info);
  const client = await api();
  const now = new Date();
  const period = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  const summary = (await (await client.get(`/api/summaries/${period}`)).json()) as { cardTotal: number; expense: number };
  await client.dispose();
  const locale = String(info.project.use.locale);
  const money = (value: number) => new Intl.NumberFormat(locale, { style: 'currency', currency: 'BRL' }).format(value).replace(/\s/g, ' ');
  await page.goto('/');
  await expect(page.getByRole('heading', { name: t('nav.overview') })).toBeVisible();
  const main = page.locator('main');
  await expect(main).toContainText(t('overview.expense'));
  await expect(main).toContainText(money(summary.expense));
  await expect(main).toContainText(money(summary.cardTotal));
});

import { expect, test } from '@playwright/test';
import { texts } from './support';

test('I18N-03, I18N-14: the language changes at once in settings and stays after a restart', async ({ page }, info) => {
  const t = texts(info);
  const other = String(info.project.use.locale) === 'en-US' ? 'pt-BR' : 'en-US';
  const otherSettings = other === 'en-US' ? 'Settings' : 'Ajustes';
  await page.goto('/settings');
  await page.getByLabel(t('settings.language')).selectOption(other);
  await page.locator('form[aria-labelledby=general-settings-title]').getByRole('button', { name: t('action.save') }).click();
  await expect(page.getByRole('heading', { name: otherSettings, level: 1 })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: otherSettings, level: 1 })).toBeVisible();
  const saveAgain = other === 'en-US' ? 'Save' : 'Salvar';
  await page.getByLabel(other === 'en-US' ? 'Language' : 'Idioma').selectOption('');
  await page.locator('form[aria-labelledby=general-settings-title]').getByRole('button', { name: saveAgain }).click();
  await expect(page.getByRole('heading', { name: t('nav.settings'), level: 1 })).toBeVisible();
});

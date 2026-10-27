import { useTranslation } from 'react-i18next';
import { CardsSection } from '../cards/CardsSection';
import { CategoriesSection } from '../categories/CategoriesSection';
import { BackupSettings } from './BackupSettings';
import { GeneralSettings } from './GeneralSettings';
import { SmtpSettings } from './SmtpSettings';
import { WorkbookSettings } from './WorkbookSettings';

/**
 * Settings screen: general options, e-mail, spreadsheet, backups, cards and categories (Q15, IMP-39).
 *
 * @returns the page
 */
export function SettingsPage() {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">{t('nav.settings')}</h1>
      <GeneralSettings />
      <SmtpSettings />
      <WorkbookSettings />
      <BackupSettings />
      <CardsSection />
      <CategoriesSection />
    </div>
  );
}

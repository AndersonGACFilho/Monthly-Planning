import { useTranslation } from 'react-i18next';
import { BackupSettings } from './BackupSettings';
import { GeneralSettings } from './GeneralSettings';
import { SmtpSettings } from './SmtpSettings';
import { UpdateSettings } from '../update/UpdateSettings';
import { WorkbookSettings } from './WorkbookSettings';

/**
 * Settings screen: general options, e-mail, spreadsheet, backups and updates; registries live in their own screen
 * (Q15, IMP-39).
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
      <UpdateSettings />
    </div>
  );
}

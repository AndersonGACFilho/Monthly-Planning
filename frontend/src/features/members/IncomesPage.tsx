import { useTranslation } from 'react-i18next';
import { MembersSection } from './MembersSection';

/**
 * Tab 2, incomes and members (SPEC section 5). Members are managed here (RF-20).
 *
 * @returns the page
 */
export function IncomesPage() {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">{t('nav.incomes')}</h1>
      <MembersSection />
    </div>
  );
}

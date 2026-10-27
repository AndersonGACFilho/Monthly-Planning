import { useTranslation } from 'react-i18next';
import { CardsSection } from '../cards/CardsSection';
import { CategoriesSection } from '../categories/CategoriesSection';
import { MembersSection } from '../members/MembersSection';

/**
 * Registries in one place: members (cotas), cards (rename by editing the nickname) and categories (RF-01 to
 * RF-08).
 *
 * @returns the page
 */
export function RegistryPage() {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col gap-8">
      <div>
        <h1 className="text-xl font-semibold">{t('nav.registry')}</h1>
        <p className="text-sm text-(--color-text-muted)">{t('registry.hint')}</p>
      </div>
      <MembersSection />
      <CardsSection />
      <CategoriesSection />
    </div>
  );
}

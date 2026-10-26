import { useTranslation } from 'react-i18next';

/** Props of {@link PlaceholderPage}. */
export interface PlaceholderPageProps {
  /** Translation key of the page title. */
  titleKey: string;
}

/**
 * Temporary page for a screen that a later milestone builds.
 *
 * @param props - {@link PlaceholderPageProps}
 * @returns a heading and a short note
 */
export function PlaceholderPage({ titleKey }: PlaceholderPageProps) {
  const { t } = useTranslation();
  return (
    <section>
      <h1 className="mb-2 text-xl font-semibold">{t(titleKey)}</h1>
      <p className="text-(--color-text-muted)">{t('page.comingSoon')}</p>
    </section>
  );
}

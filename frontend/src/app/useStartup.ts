import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { createDefaultCategories } from '../api/generated/endpoints/category-controller/category-controller';
import { settings as getSettings } from '../api/generated/endpoints/settings-controller/settings-controller';
import i18n, { detectLanguage, type Language } from '../i18n/i18n';

let started = false;

/**
 * Lets tests run the startup again.
 */
export function resetStartup(): void {
  started = false;
}

/**
 * First steps after the local service answers: apply the saved language (I18N-03) and create the initial
 * categories in that language on first use (I18N-07; the backend does nothing when categories exist).
 */
export function useStartup(): void {
  const queryClient = useQueryClient();
  useEffect(() => {
    if (started) return;
    started = true;
    void (async () => {
      const settings = (await getSettings()) as unknown as Record<string, string>;
      const language = (settings.language || detectLanguage(navigator.languages)) as Language;
      await i18n.changeLanguage(language);
      const created = await createDefaultCategories({ language });
      if (created._embedded) await queryClient.invalidateQueries();
    })().catch(() => undefined);
  }, [queryClient]);
}

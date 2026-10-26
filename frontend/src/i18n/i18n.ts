import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import enUS from '../locales/en-US.json';
import ptBR from '../locales/pt-BR.json';

/** Languages of the interface (I18N-03). */
export const SUPPORTED_LANGUAGES = ['pt-BR', 'en-US'] as const;

/** One of {@link SUPPORTED_LANGUAGES}. */
export type Language = (typeof SUPPORTED_LANGUAGES)[number];

/** Language used when the system language is not supported. */
export const DEFAULT_LANGUAGE: Language = 'pt-BR';

/**
 * Picks the interface language from the system preferences, as reported by the window.
 *
 * @param preferred - languages in order of preference, such as `navigator.languages`
 * @returns the first supported language, matching by language when the region differs
 */
export function detectLanguage(preferred: readonly string[]): Language {
  for (const tag of preferred) {
    const exact = SUPPORTED_LANGUAGES.find((lang) => lang.toLowerCase() === tag.toLowerCase());
    if (exact) return exact;
    const sameLanguage = SUPPORTED_LANGUAGES.find(
      (lang) => lang.split('-')[0] === tag.split('-')[0]?.toLowerCase(),
    );
    if (sameLanguage) return sameLanguage;
  }
  return DEFAULT_LANGUAGE;
}

void i18n.use(initReactI18next).init({
  resources: {
    'pt-BR': { translation: ptBR },
    'en-US': { translation: enUS },
  },
  lng: detectLanguage(typeof navigator === 'undefined' ? [] : navigator.languages),
  fallbackLng: DEFAULT_LANGUAGE,
  supportedLngs: [...SUPPORTED_LANGUAGES],
  interpolation: { escapeValue: false },
  returnNull: false,
});

export default i18n;

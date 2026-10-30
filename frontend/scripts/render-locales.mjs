// Renders each language file from scripts/locales-source.mjs; shared by build-locales and check-locales.
import source from './locales-source.mjs';

const LANGUAGES = ['pt-BR', 'en-US'];

const pick = (node, index) =>
  Array.isArray(node) ? node[index] : Object.fromEntries(Object.entries(node).map(([k, v]) => [k, pick(v, index)]));

export const renderLocales = () =>
  new Map(LANGUAGES.map((language, index) => [language, JSON.stringify(pick(source, index), null, 2) + '\n']));

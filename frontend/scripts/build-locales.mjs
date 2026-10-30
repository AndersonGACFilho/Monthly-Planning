// Writes src/locales/pt-BR.json and en-US.json from scripts/locales-source.mjs (I18N-04, I18N-13).
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { renderLocales } from './render-locales.mjs';

for (const [language, text] of renderLocales()) {
  writeFileSync(fileURLToPath(new URL(`../src/locales/${language}.json`, import.meta.url)), text);
}
console.log('Locales written');

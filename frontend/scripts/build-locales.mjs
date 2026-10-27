// Writes src/locales/pt-BR.json and en-US.json from scripts/locales-source.mjs (I18N-04, I18N-13).
import { writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import source from './locales-source.mjs';

const pick = (node, index) =>
  Array.isArray(node) ? node[index] : Object.fromEntries(Object.entries(node).map(([k, v]) => [k, pick(v, index)]));

for (const [index, language] of [[0, 'pt-BR'], [1, 'en-US']]) {
  const file = fileURLToPath(new URL(`../src/locales/${language}.json`, import.meta.url));
  writeFileSync(file, JSON.stringify(pick(source, index), null, 2) + '\n');
}
console.log('Locales written');

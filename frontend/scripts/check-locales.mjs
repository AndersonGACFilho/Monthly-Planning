// I18N-13: every translation key exists in every language file.
import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const dir = fileURLToPath(new URL('../src/locales/', import.meta.url));
const flatten = (object, prefix = '') =>
  Object.entries(object).flatMap(([key, value]) =>
    value && typeof value === 'object' ? flatten(value, `${prefix}${key}.`) : [`${prefix}${key}`],
  );

const files = readdirSync(dir).filter((name) => name.endsWith('.json'));
const keys = new Map(files.map((name) => [name, new Set(flatten(JSON.parse(readFileSync(join(dir, name), 'utf8'))))]));
const all = new Set([...keys.values()].flatMap((set) => [...set]));
let missing = 0;
for (const [name, set] of keys) {
  for (const key of all) {
    if (!set.has(key)) {
      console.error(`${name}: missing key ${key}`);
      missing++;
    }
  }
}
if (missing > 0) process.exit(1);
console.log(`Locales OK: ${files.join(', ')} share ${all.size} keys`);

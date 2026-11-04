import type { TemplateSettings, TemplateSettingsBlocksItem, TemplateSettingsFont, TemplateTexts } from '../../api/generated/model';
import { STATEMENT_BLOCKS } from '../../shared/components/BlockOrderList';

const FONTS: readonly TemplateSettingsFont[] = ['NOTO_SANS', 'NOTO_SERIF', 'LIBERATION_SANS'];
const LANGUAGES = ['pt-BR', 'en-US'] as const;
const TEXT_FIELDS = ['header', 'footer', 'message', 'emailSubject', 'emailBody', 'whatsapp'] as const;
const CODE_FIELDS = ['advancedCss', 'advancedHtml', 'layoutHtml'] as const;
const COLOR = /^#[0-9A-Fa-f]{6}$/;

/** Why a pasted template could not be read. */
export type TemplateJsonError = 'invalidJson' | 'notObject';

/** Result of reading a pasted template. */
export type TemplateJsonResult =
  | { ok: true; settings: TemplateSettings; ignored: string[] }
  | { ok: false; error: TemplateJsonError };

/**
 * Writes the settings a template can exchange as JSON: everything but the logo, which stays in the app (IMP-61).
 *
 * @param settings - template settings
 * @returns pretty JSON
 */
export function toTemplateJson(settings: TemplateSettings): string {
  const texts = Object.fromEntries(
    Object.entries(settings.texts ?? {}).map(([language, value]) => [
      language,
      Object.fromEntries(TEXT_FIELDS.filter((field) => value[field] !== undefined).map((field) => [field, value[field]])),
    ]),
  );
  const shared = {
    primaryColor: settings.primaryColor,
    secondaryColor: settings.secondaryColor,
    font: settings.font,
    blocks: settings.blocks,
    texts,
  };
  const code = Object.fromEntries(CODE_FIELDS.map((field) => [field, settings[field] ?? null]));
  return JSON.stringify({ ...shared, ...code }, null, 2);
}

/**
 * Reads a template written elsewhere, such as the answer of a text generator, over the current settings (IMP-61).
 * Code fences and text around the JSON object are skipped. Known fields with a valid shape replace the current
 * ones; anything else is ignored and reported, and the logo is always kept. The server checks the rest when the
 * preview renders.
 *
 * @param text    - pasted text
 * @param current - settings being edited
 * @returns the new settings and the ignored fields, or why the text could not be read
 */
export function applyTemplateJson(text: string, current: TemplateSettings): TemplateJsonResult {
  const start = text.indexOf('{');
  const end = text.lastIndexOf('}');
  if (start < 0 || end <= start) return { ok: false, error: 'invalidJson' };
  let parsed: unknown;
  try {
    parsed = JSON.parse(text.slice(start, end + 1));
  } catch {
    return { ok: false, error: 'invalidJson' };
  }
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return { ok: false, error: 'notObject' };
  const input = parsed as Record<string, unknown>;
  const settings: TemplateSettings = { ...current };
  const ignored: string[] = [];
  const isString = (value: unknown): value is string => typeof value === 'string';

  for (const [key, value] of Object.entries(input)) {
    if ((key === 'primaryColor' || key === 'secondaryColor') && isString(value) && COLOR.test(value)) {
      settings[key] = value.toUpperCase();
    } else if (key === 'font' && FONTS.includes(value as TemplateSettingsFont)) {
      settings.font = value as TemplateSettingsFont;
    } else if (key === 'blocks' && Array.isArray(value) && value.length > 0
        && value.every((b) => STATEMENT_BLOCKS.includes(b as TemplateSettingsBlocksItem))) {
      settings.blocks = [...new Set(value as TemplateSettingsBlocksItem[])];
    } else if (key === 'texts' && value && typeof value === 'object') {
      settings.texts = mergeTexts(current.texts ?? {}, value as Record<string, unknown>, ignored);
    } else if ((CODE_FIELDS as readonly string[]).includes(key) && (isString(value) || value === null)) {
      settings[key as (typeof CODE_FIELDS)[number]] = isString(value) && value.trim() ? value : undefined;
    } else {
      ignored.push(key);
    }
  }
  return { ok: true, settings, ignored };
}

function mergeTexts(
  current: Record<string, TemplateTexts>,
  input: Record<string, unknown>,
  ignored: string[],
): Record<string, TemplateTexts> {
  const result = { ...current };
  for (const [language, value] of Object.entries(input)) {
    if (!(LANGUAGES as readonly string[]).includes(language) || !value || typeof value !== 'object') {
      ignored.push(`texts.${language}`);
      continue;
    }
    const texts: TemplateTexts = { ...(result[language] ?? {}) };
    for (const [field, text] of Object.entries(value as Record<string, unknown>)) {
      if ((TEXT_FIELDS as readonly string[]).includes(field) && typeof text === 'string') {
        texts[field as (typeof TEXT_FIELDS)[number]] = text;
      } else {
        ignored.push(`texts.${language}.${field}`);
      }
    }
    result[language] = texts;
  }
  return result;
}

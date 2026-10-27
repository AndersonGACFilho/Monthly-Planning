import { readFileSync } from 'node:fs';
import { request, type APIRequestContext, type TestInfo } from '@playwright/test';

/** Local API of the end-to-end backend. */
export const API = 'http://127.0.0.1:18090';

/**
 * Creates an API client with the session token.
 *
 * @returns request context
 */
export function api(): Promise<APIRequestContext> {
  return request.newContext({ baseURL: API, extraHTTPHeaders: { 'X-Session-Token': 'e2e' } });
}

type Messages = Record<string, unknown>;

/**
 * Returns the interface text of a key in the language of the test project, read from the same locale files the
 * app uses, so one spec checks both languages (I18N-14).
 *
 * @param info - test info
 * @returns translator
 */
export function texts(info: TestInfo): (key: string, values?: Record<string, string | number>) => string {
  const language = String(info.project.use.locale ?? 'pt-BR') === 'en-US' ? 'en-US' : 'pt-BR';
  const messages = JSON.parse(readFileSync(new URL(`../src/locales/${language}.json`, import.meta.url), 'utf-8')) as Messages;
  return (key, values = {}) => {
    const found = key.split('.').reduce<unknown>((node, part) => (node as Messages | undefined)?.[part], messages);
    if (typeof found !== 'string') throw new Error(`Missing text ${key}`);
    return Object.entries(values).reduce((text, [name, value]) => text.replaceAll(`{{${name}}}`, String(value)), found);
  };
}

/**
 * Tells whether the project runs the wide layout (1200 px or more).
 *
 * @param info - test info
 * @returns whether the viewport is wide
 */
export function isWide(info: TestInfo): boolean {
  return (info.project.use.viewport?.width ?? 1280) >= 1200;
}

import type { TFunction } from 'i18next';
import type { LayoutReference, TemplateSettings } from '../../api/generated/model';
import { toTemplateJson } from './templateJson';

/** What the guide is built from. */
export interface TemplateBriefInput {
  /** Placeholders, variables, classes, ids and rules of the advanced mode. */
  reference: LayoutReference;
  /** Template being edited. */
  settings: TemplateSettings;
  /** Statement rendered with sample data and the current settings, never real data. */
  sampleHtml: string;
}

/** Shape of the answer, with the allowed values. */
function answerShape(reference: LayoutReference): string {
  const rules = reference.rules ?? {};
  const texts = '{ "header": "…", "footer": "…", "message": "…", "emailSubject": "…", "emailBody": "…", "whatsapp": "…" }';
  return [
    '{',
    '  "primaryColor": "#RRGGBB",',
    '  "secondaryColor": "#RRGGBB",',
    `  "font": "${Object.keys(rules.fonts ?? {}).join(' | ')}",`,
    `  "blocks": [${(rules.blocks ?? []).map((b) => `"${b}"`).join(', ')}],`,
    `  "texts": { ${(rules.languages ?? []).map((l) => `"${l}": ${texts}`).join(', ')} },`,
    '  "advancedCss": "…",',
    '  "advancedHtml": "…",',
    '  "layoutHtml": "…"',
    '}',
  ].join('\n');
}

/** Removes embedded images, such as the logo, which only make the guide longer. */
function withoutDataUris(html: string): string {
  return html.replace(/data:image\/[a-z]+;base64,[A-Za-z0-9+/=]+/g, 'data:image/…');
}

const fence = (language: string, body: string) => `\`\`\`${language}\n${body.trim()}\n\`\`\``;

/**
 * Builds a guide, in the interface language, with everything another tool needs to write a statement template that
 * this app accepts: the task and the answer format, the sanitizer and PDF rules, placeholders, variables, every
 * class and id, the standard layout, the current template and the statement HTML with sample data (IMP-61).
 *
 * @param input - reference, settings and sample HTML
 * @param t     - translation function
 * @returns Markdown text
 */
export function buildTemplateBrief({ reference, settings, sampleHtml }: TemplateBriefInput, t: TFunction): string {
  const rules = reference.rules ?? {};
  const list = (values: string[] | undefined) => (values ?? []).map((v) => `\`${v}\``).join(', ');
  const fonts = Object.entries(rules.fonts ?? {}).map(([value, family]) => `\`${value}\` (${family})`).join(', ');
  const sections = [
    `# ${t('templates.guide.title')}`,
    t('templates.guide.task'),
    `## ${t('templates.guide.answer')}`,
    t('templates.guide.answerHint'),
    fence('json', answerShape(reference)),
    `## ${t('templates.guide.rules')}`,
    [
      t('templates.guide.ruleHtml', { tags: list(rules.htmlTags), attributes: list(rules.htmlAttributes) }),
      t('templates.guide.ruleLayout', { tags: list(rules.layoutTags), attributes: list(rules.layoutAttributes) }),
      t('templates.guide.ruleCss', { forbidden: list(rules.forbiddenCss) }),
      t('templates.guide.ruleSizes', { css: rules.maxCss, header: rules.maxHeaderHtml, layout: rules.maxLayout }),
      t('templates.guide.rulePdf'),
      t('templates.guide.ruleFonts', { fonts }),
      t('templates.guide.ruleBlocks', { blocks: list(rules.blocks) }),
      t('templates.guide.rulePlaceholders', { placeholders: list(reference.placeholders?.map((p) => `{{${p}}}`)) }),
      t('templates.guide.ruleVariables', { variables: list(reference.variables?.map((v) => `{{${v}}}`)) }),
      t('templates.guide.ruleTexts', { languages: list(rules.languages) }),
      t('templates.guide.ruleHeader'),
      t('templates.guide.ruleLogo'),
    ]
      .map((rule) => `- ${rule}`)
      .join('\n'),
    `## ${t('templates.guide.selectors')}`,
    (reference.selectors ?? []).map((s) => `- \`${s.selector}\`: ${s.description}`).join('\n'),
    `## ${t('templates.guide.standardLayout')}`,
    fence('html', reference.standardLayout ?? ''),
    `## ${t('templates.guide.current')}`,
    fence('json', toTemplateJson(settings)),
    `## ${t('templates.guide.sample')}`,
    t('templates.guide.sampleHint'),
    fence('html', withoutDataUris(sampleHtml)),
    `## ${t('templates.guide.request')}`,
    t('templates.guide.requestHint'),
  ];
  return `${sections.join('\n\n')}\n`;
}

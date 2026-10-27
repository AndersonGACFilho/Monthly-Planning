import { ArrowDown, ArrowUp } from 'lucide-react';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  createTemplate,
  previewTemplate,
  replaceTemplate,
} from '../../api/generated/endpoints/template-controller/template-controller';
import type {
  MemberModel,
  TemplateModel,
  TemplatePreviewModel,
  TemplateSettings,
  TemplateSettingsBlocksItem,
  TemplateTexts,
} from '../../api/generated/model';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';

const BLOCKS: TemplateSettingsBlocksItem[] = ['SUMMARY', 'CARDS', 'FIXED_EXPENSES', 'VARIABLE_EXPENSES', 'COMMITTED', 'PAYMENT', 'NOTE'];
const FONTS = [
  ['NOTO_SANS', 'Noto Sans'],
  ['NOTO_SERIF', 'Noto Serif'],
  ['LIBERATION_SANS', 'Liberation Sans'],
] as const;
const LANGUAGES = ['pt-BR', 'en-US'] as const;
const TEXT_FIELDS = ['header', 'footer', 'message', 'emailSubject', 'emailBody', 'whatsapp'] as const;
const VARIABLES = '{{member.name}}, {{billingPeriod}}, {{total}}, {{dueDate}}, {{pixKey}}';
const MAX_LOGO_BYTES = 500 * 1024;
const PREVIEW_DELAY_MS = 400;

/** Props of {@link TemplateEditor}. */
export interface TemplateEditorProps {
  /** Template being edited, or `undefined` for a new one based on `base`. */
  template?: TemplateModel;
  /** Settings a new template starts from. */
  base: TemplateSettings;
  /** Billing period for the preview with real data. */
  period: string;
  /** Called after saving or cancelling. */
  onDone: (saved?: TemplateModel) => void;
}

/**
 * Template editor without code: colours, font, logo, blocks and their order, texts per language with English
 * variables and an optional advanced HTML/CSS mode, with a live preview on real or sample data (TPL-02, TPL-03,
 * TPL-05, TPL-07, I18N-11).
 *
 * @param props - {@link TemplateEditorProps}
 * @returns the editor
 */
export function TemplateEditor({ template, base, period, onDone }: TemplateEditorProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const { data: memberPage } = useMembers({ size: 200, sort: ['code,asc'] });
  const members = embedded<MemberModel>(memberPage, 'members');
  const [name, setName] = useState(template?.name ?? t('templates.newName'));
  const [settings, setSettings] = useState<TemplateSettings>(template?.settings ?? base);
  const [textLanguage, setTextLanguage] = useState<(typeof LANGUAGES)[number]>('pt-BR');
  const [previewLanguage, setPreviewLanguage] = useState<(typeof LANGUAGES)[number]>('pt-BR');
  const [previewMember, setPreviewMember] = useState<number | null>(null);
  const [preview, setPreview] = useState<TemplatePreviewModel | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      previewTemplate({ settings, billingPeriod: period, memberId: previewMember ?? undefined, language: previewLanguage })
        .then((result) => {
          setPreview(result);
          setError(null);
        })
        .catch((e: unknown) => setError(e instanceof Error ? e.message : String(e)));
    }, PREVIEW_DELAY_MS);
    return () => clearTimeout(timer);
  }, [settings, period, previewMember, previewLanguage]);

  const update = (change: Partial<TemplateSettings>) => setSettings((s) => ({ ...s, ...change }));
  const blocks = settings.blocks ?? [];
  const texts: TemplateTexts = (settings.texts as Record<string, TemplateTexts> | undefined)?.[textLanguage] ?? {};
  const setText = (field: (typeof TEXT_FIELDS)[number], value: string) =>
    update({ texts: { ...(settings.texts ?? {}), [textLanguage]: { ...texts, [field]: value } } });
  const move = (index: number, delta: number) => {
    const next = [...blocks];
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item!);
    update({ blocks: next });
  };
  const toggleBlock = (block: TemplateSettingsBlocksItem) =>
    update({ blocks: blocks.includes(block) ? blocks.filter((b) => b !== block) : [...blocks, block] });

  const save = async () => {
    try {
      const result = template?.id !== undefined
        ? await replaceTemplate(template.id, { name, settings }, ifMatch(template.version))
        : await createTemplate({ name, settings });
      await saved();
      onDone(result);
    } catch (e) {
      reportError(e);
    }
  };

  return (
    <div className="grid gap-4 wide:grid-cols-2">
      <form
        className="flex flex-col gap-4"
        aria-label={t('templates.look')}
        onSubmit={(event) => {
          event.preventDefault();
          void save();
        }}
      >
        <fieldset className="panel grid grid-cols-2 gap-3">
          <legend className="font-semibold">{t('templates.look')}</legend>
          <Field label={t('templates.name')} htmlFor="template-name">
            <input id="template-name" className="input" value={name} maxLength={80} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label={t('templates.font')} htmlFor="template-font">
            <select id="template-font" className="input" value={settings.font} onChange={(e) => update({ font: e.target.value as TemplateSettings['font'] })}>
              {FONTS.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
          <Field label={t('templates.primaryColor')} htmlFor="template-primary">
            <input id="template-primary" type="color" className="input h-9" value={settings.primaryColor} onChange={(e) => update({ primaryColor: e.target.value.toUpperCase() })} />
          </Field>
          <Field label={t('templates.secondaryColor')} htmlFor="template-secondary">
            <input id="template-secondary" type="color" className="input h-9" value={settings.secondaryColor} onChange={(e) => update({ secondaryColor: e.target.value.toUpperCase() })} />
          </Field>
          <Field label={t('templates.logo')} htmlFor="template-logo">
            <input
              id="template-logo"
              type="file"
              accept="image/png,image/jpeg"
              className="input"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                if (file.size > MAX_LOGO_BYTES) {
                  setError(t('templates.logoTooBig'));
                  return;
                }
                const reader = new FileReader();
                reader.onload = () => update({ logo: String(reader.result) });
                reader.readAsDataURL(file);
              }}
            />
          </Field>
          {settings.logo && (
            <div className="flex items-end gap-2">
              <img src={settings.logo} alt="" className="max-h-10" />
              <button type="button" className="button" onClick={() => update({ logo: undefined })}>
                {t('templates.removeLogo')}
              </button>
            </div>
          )}
        </fieldset>

        <fieldset className="panel flex flex-col gap-1">
          <legend className="font-semibold">{t('templates.blocks')}</legend>
          {[...blocks, ...BLOCKS.filter((b) => !blocks.includes(b))].map((block) => {
            const index = blocks.indexOf(block);
            return (
              <div key={block} className="flex items-center gap-2">
                <label className="flex flex-1 items-center gap-2">
                  <input type="checkbox" checked={index >= 0} onChange={() => toggleBlock(block)} />
                  {t(`templates.blockNames.${block}`)}
                </label>
                <button type="button" className="icon-button" aria-label={t('templates.moveUp')} disabled={index <= 0} onClick={() => move(index, -1)}>
                  <ArrowUp size={14} aria-hidden />
                </button>
                <button
                  type="button"
                  className="icon-button"
                  aria-label={t('templates.moveDown')}
                  disabled={index < 0 || index === blocks.length - 1}
                  onClick={() => move(index, 1)}
                >
                  <ArrowDown size={14} aria-hidden />
                </button>
              </div>
            );
          })}
        </fieldset>

        <fieldset className="panel flex flex-col gap-2">
          <legend className="font-semibold">{t('templates.texts')}</legend>
          <div className="flex gap-2" role="tablist">
            {LANGUAGES.map((lang) => (
              <button
                key={lang}
                type="button"
                role="tab"
                aria-selected={textLanguage === lang}
                className={`button ${textLanguage === lang ? 'button-primary' : ''}`}
                onClick={() => setTextLanguage(lang)}
              >
                {t(`language.${lang}`)}
              </button>
            ))}
          </div>
          <p className="text-xs text-(--color-text-muted)">{t('templates.variables', { list: VARIABLES })}</p>
          {TEXT_FIELDS.map((field) => (
            <Field key={field} label={t(`templates.textFields.${field}`)} htmlFor={`template-${field}`}>
              {field === 'emailBody' || field === 'whatsapp' || field === 'message' ? (
                <textarea id={`template-${field}`} className="input min-h-16" value={texts[field] ?? ''} onChange={(e) => setText(field, e.target.value)} />
              ) : (
                <input id={`template-${field}`} className="input" value={texts[field] ?? ''} onChange={(e) => setText(field, e.target.value)} />
              )}
            </Field>
          ))}
        </fieldset>

        <details className="panel">
          <summary className="cursor-pointer font-semibold">{t('templates.advanced')}</summary>
          <p className="my-2 text-xs text-(--color-text-muted)">{t('templates.advancedHint')}</p>
          <Field label={t('templates.advancedHtml')} htmlFor="template-html">
            <textarea id="template-html" className="input min-h-20 font-mono text-xs" value={settings.advancedHtml ?? ''} onChange={(e) => update({ advancedHtml: e.target.value || undefined })} />
          </Field>
          <Field label={t('templates.advancedCss')} htmlFor="template-css">
            <textarea id="template-css" className="input min-h-20 font-mono text-xs" value={settings.advancedCss ?? ''} onChange={(e) => update({ advancedCss: e.target.value || undefined })} />
          </Field>
        </details>

        {error && (
          <p role="alert" className="text-sm text-red-700">
            {error}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <button type="button" className="button" onClick={() => onDone()}>
            {t('action.cancel')}
          </button>
          <button type="submit" className="button button-primary">
            {t('action.save')}
          </button>
        </div>
      </form>

      <section className="flex flex-col gap-2" aria-labelledby="template-preview-title">
        <h2 id="template-preview-title" className="font-semibold">
          {t('templates.preview')}
        </h2>
        <div className="flex flex-wrap gap-3">
          <Field label={t('templates.previewData')} htmlFor="preview-member">
            <select
              id="preview-member"
              className="input"
              value={previewMember ?? ''}
              onChange={(e) => setPreviewMember(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">{t('templates.sampleData')}</option>
              {members.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.code} — {m.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label={t('templates.previewLanguage')} htmlFor="preview-language">
            <select id="preview-language" className="input" value={previewLanguage} onChange={(e) => setPreviewLanguage(e.target.value as (typeof LANGUAGES)[number])}>
              {LANGUAGES.map((lang) => (
                <option key={lang} value={lang}>
                  {t(`language.${lang}`)}
                </option>
              ))}
            </select>
          </Field>
        </div>
        {preview?.fallback && (
          <p className="text-sm text-amber-800" role="status">
            {t('templates.fallback')}
          </p>
        )}
        {preview?.subject && (
          <p className="text-sm">
            <strong>{t('templates.subject')}:</strong> {preview.subject}
          </p>
        )}
        <iframe
          title={t('templates.preview')}
          sandbox=""
          className="h-[70vh] w-full rounded border border-(--color-border) bg-white"
          srcDoc={preview?.html ?? ''}
        />
      </section>
    </div>
  );
}

import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useTemplateLayoutReference } from '../../api/generated/endpoints/template-controller/template-controller';
import type { TemplateSettings } from '../../api/generated/model';
import { Field } from '../../shared/components/Field';
import { insertText } from './insertText';
import { LayoutReferencePanel } from './LayoutReferencePanel';

type AdvancedField = 'layoutHtml' | 'advancedHtml' | 'advancedCss';

/** Props of {@link AdvancedLayout}. */
export interface AdvancedLayoutProps {
  /** Settings being edited. */
  settings: TemplateSettings;
  /** Merges a change into the settings. */
  update: (change: Partial<TemplateSettings>) => void;
}

/**
 * Advanced template mode (TPL-07): a full layout that replaces the whole statement body and places its parts with
 * placeholders, the header HTML used by the standard layout and `{{header}}`, extra CSS, and the reference of every
 * placeholder, variable, class and id.
 *
 * @param props - {@link AdvancedLayoutProps}
 * @returns the section
 */
export function AdvancedLayout({ settings, update }: AdvancedLayoutProps) {
  const { t } = useTranslation();
  const { data: reference } = useTemplateLayoutReference();
  const layoutField = useRef<HTMLTextAreaElement>(null);
  const cssField = useRef<HTMLTextAreaElement>(null);
  const set = (field: AdvancedField, value: string) => update({ [field]: value || undefined });
  const insert = (field: 'layoutHtml' | 'advancedCss', snippet: string) => {
    const element = (field === 'layoutHtml' ? layoutField : cssField).current;
    const result = insertText(settings[field] ?? '', snippet, element?.selectionStart ?? null, element?.selectionEnd ?? null);
    set(field, result.value);
    requestAnimationFrame(() => {
      element?.focus();
      element?.setSelectionRange(result.caret, result.caret);
    });
  };
  const startFromStandard = () => {
    if (settings.layoutHtml && !window.confirm(t('templates.layout.confirmReplace'))) return;
    set('layoutHtml', reference?.standardLayout ?? '');
  };
  const hasLayout = Boolean(settings.layoutHtml?.trim());

  return (
    <details className="panel" open={hasLayout || undefined}>
      <summary className="cursor-pointer font-semibold">{t('templates.advanced')}</summary>
      <p className="my-2 text-xs text-(--color-text-muted)">{t('templates.advancedHint')}</p>
      <div className="flex flex-col gap-3">
        <Field label={t('templates.layout.html')} htmlFor="template-layout">
          <textarea
            id="template-layout"
            ref={layoutField}
            className="input min-h-48 font-mono text-xs"
            spellCheck={false}
            placeholder={t('templates.layout.empty')}
            value={settings.layoutHtml ?? ''}
            onChange={(e) => set('layoutHtml', e.target.value)}
          />
        </Field>
        <p className="text-xs text-(--color-text-muted)">{t(hasLayout ? 'templates.layout.active' : 'templates.layout.hint')}</p>
        <div className="flex flex-wrap gap-2">
          <button type="button" className="button" disabled={!reference?.standardLayout} onClick={startFromStandard}>
            {t('templates.layout.startFromStandard')}
          </button>
          {hasLayout && (
            <button type="button" className="button" onClick={() => set('layoutHtml', '')}>
              {t('templates.layout.remove')}
            </button>
          )}
        </div>
        <Field label={t('templates.advancedHtml')} htmlFor="template-html">
          <textarea
            id="template-html"
            className="input min-h-20 font-mono text-xs"
            spellCheck={false}
            value={settings.advancedHtml ?? ''}
            onChange={(e) => set('advancedHtml', e.target.value)}
          />
        </Field>
        <Field label={t('templates.advancedCss')} htmlFor="template-css">
          <textarea
            id="template-css"
            ref={cssField}
            className="input min-h-32 font-mono text-xs"
            spellCheck={false}
            value={settings.advancedCss ?? ''}
            onChange={(e) => set('advancedCss', e.target.value)}
          />
        </Field>
        <LayoutReferencePanel
          onInsertPlaceholder={(placeholder) => insert('layoutHtml', placeholder)}
          onInsertSelector={(selector) => insert('advancedCss', `\n${selector} {\n  \n}\n`)}
        />
      </div>
    </details>
  );
}

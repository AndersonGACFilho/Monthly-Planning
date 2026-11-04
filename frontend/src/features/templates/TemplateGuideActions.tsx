import { ClipboardCopy, FileJson } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  previewTemplate,
  useTemplateLayoutReference,
} from '../../api/generated/endpoints/template-controller/template-controller';
import type { TemplateSettings } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { Modal } from '../../shared/components/Modal';
import { copyText, downloadFile } from '../../shared/desktop';
import { useNotifications } from '../../shared/state/notifications';
import { applyTemplateJson } from './templateJson';
import { buildTemplateBrief } from './templateBrief';

/** Props of {@link TemplateGuideActions}. */
export interface TemplateGuideActionsProps {
  /** Settings being edited. */
  settings: TemplateSettings;
  /** Billing period of the sample statement. */
  period: string;
  /** Replaces the settings being edited; nothing is saved. */
  onApply: (settings: TemplateSettings) => void;
}

/**
 * Exchange with a text generator such as an LLM (IMP-61): copies a guide with everything needed to write a
 * template this app accepts, built from sample data only, and applies the JSON answer to the editor for review.
 *
 * @param props - {@link TemplateGuideActionsProps}
 * @returns the two buttons and the paste dialog
 */
export function TemplateGuideActions({ settings, period, onApply }: TemplateGuideActionsProps) {
  const { t, i18n } = useTranslation();
  const push = useNotifications((state) => state.push);
  const { data: reference } = useTemplateLayoutReference();
  const [pasting, setPasting] = useState(false);
  const [pasted, setPasted] = useState('');
  const [error, setError] = useState<string | null>(null);

  const copyGuide = async () => {
    if (!reference) return;
    let guide: string;
    try {
      // No member: the guide must never carry real data
      const sample = await previewTemplate({ settings, billingPeriod: period, language: i18n.language as 'pt-BR' | 'en-US' });
      guide = buildTemplateBrief({ reference, settings, sampleHtml: sample.html ?? '' }, t);
    } catch (e) {
      reportError(e);
      return;
    }
    try {
      await copyText(guide);
      push({ kind: 'success', title: t('templates.guide.copied', { count: guide.length }) });
    } catch {
      downloadFile(new Blob([guide], { type: 'text/markdown' }), 'guia-template.md');
      push({ kind: 'success', title: t('templates.guide.downloaded') });
    }
  };

  const apply = () => {
    const result = applyTemplateJson(pasted, settings);
    if (!result.ok) {
      setError(t(`templates.guide.errors.${result.error}`));
      return;
    }
    onApply(result.settings);
    push({
      kind: 'success',
      title: result.ignored.length > 0
        ? t('templates.guide.appliedIgnoring', { fields: result.ignored.join(', ') })
        : t('templates.guide.applied'),
    });
    setPasting(false);
    setPasted('');
    setError(null);
  };

  return (
    <div className="flex flex-wrap items-center gap-2">
      <button type="button" className="button" disabled={!reference} title={t('templates.guide.copyHint')} onClick={() => void copyGuide()}>
        <ClipboardCopy size={14} aria-hidden /> {t('templates.guide.copy')}
      </button>
      <button type="button" className="button" onClick={() => setPasting(true)}>
        <FileJson size={14} aria-hidden /> {t('templates.guide.apply')}
      </button>
      {pasting && (
        <Modal title={t('templates.guide.applyTitle')} onClose={() => setPasting(false)}>
          <div className="flex flex-col gap-3 p-2">
            <p className="text-sm text-(--color-text-muted)">{t('templates.guide.applyHint')}</p>
            <Field label={t('templates.guide.applyLabel')} htmlFor="template-json">
              <textarea
                id="template-json"
                className="input min-h-64 font-mono text-xs"
                spellCheck={false}
                value={pasted}
                onChange={(e) => {
                  setPasted(e.target.value);
                  setError(null);
                }}
              />
            </Field>
            {error && (
              <p role="alert" className="text-sm text-red-700 dark:text-red-400">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <button type="button" className="button" onClick={() => setPasting(false)}>
                {t('action.cancel')}
              </button>
              <button type="button" className="button button-primary" disabled={!pasted.trim()} onClick={apply}>
                {t('templates.guide.applyButton')}
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}

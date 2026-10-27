import { FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMonthlySummary } from '../../api/generated/endpoints/monthly-summary-controller/monthly-summary-controller';
import { useWorkbookOptions } from '../../api/generated/endpoints/workbook-controller/workbook-controller';
import { apiFetchFile } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { downloadFile } from '../../shared/desktop';
import { useBillingPeriod } from '../../shared/state/billingPeriod';

type Mode = 'EXCEL_365' | 'COMPATIBLE';

/**
 * Button beside the billing period that exports the monthly spreadsheet: formula mode (default by system),
 * language and the warning about entries pending review (XLS-01, XLS-07, XLS-09, XLS-10).
 *
 * @returns the button and its dialog
 */
export function WorkbookExportButton() {
  const { t, i18n } = useTranslation();
  const period = useBillingPeriod();
  const [open, setOpen] = useState(false);
  const [mode, setMode] = useState<Mode | ''>('');
  const [language, setLanguage] = useState('');
  const [busy, setBusy] = useState(false);
  const { data: summary } = useMonthlySummary(period, undefined, { query: { enabled: open } });
  const { data: options } = useWorkbookOptions(period, undefined, { query: { enabled: open } });
  const defaultMode = (options?.defaultMode ?? 'COMPATIBLE') as Mode;
  const chosen = mode || defaultMode;
  const pending = summary?.pendingCount ?? 0;

  const download = async () => {
    setBusy(true);
    try {
      const lang = language || (i18n.language === 'en-US' ? 'en-US' : 'pt-BR');
      const file = await apiFetchFile(`/api/summaries/${period}/workbook?mode=${chosen}&language=${lang}`);
      downloadFile(file.blob, file.fileName ?? `${period}.xlsx`);
      setOpen(false);
    } catch (error) {
      reportError(error);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="relative">
      <button type="button" className="button" onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        <FileSpreadsheet size={16} aria-hidden /> {t('workbook.export')}
      </button>
      {open && (
        <div className="panel absolute right-0 z-20 mt-2 flex w-80 flex-col gap-3 shadow-lg" role="dialog" aria-label={t('workbook.title')}>
          <Field label={t('workbook.mode')} htmlFor="workbook-mode">
            <select id="workbook-mode" className="input" value={chosen} onChange={(e) => setMode(e.target.value as Mode)}>
              <option value="EXCEL_365">{t('workbook.modes.EXCEL_365')}</option>
              <option value="COMPATIBLE">{t('workbook.modes.COMPATIBLE')}</option>
            </select>
          </Field>
          <p className="text-xs text-(--color-text-muted)">{t('workbook.defaultMode', { mode: t(`workbook.modes.${defaultMode}`) })}</p>
          <Field label={t('workbook.language')} htmlFor="workbook-language">
            <select id="workbook-language" className="input" value={language} onChange={(e) => setLanguage(e.target.value)}>
              <option value="">{t(`language.${i18n.language === 'en-US' ? 'en-US' : 'pt-BR'}`)}</option>
              <option value="pt-BR">{t('language.pt-BR')}</option>
              <option value="en-US">{t('language.en-US')}</option>
            </select>
          </Field>
          {pending > 0 && (
            <p className="text-sm text-amber-800" role="status">
              {t('workbook.pending', { count: pending })}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <button type="button" className="button" onClick={() => setOpen(false)}>
              {t('action.cancel')}
            </button>
            <button type="button" className="button button-primary" disabled={busy} onClick={() => void download()}>
              {t('workbook.download')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

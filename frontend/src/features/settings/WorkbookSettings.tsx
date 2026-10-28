import { ArrowDown, ArrowUp } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import { changeSettings } from '../../api/generated/endpoints/settings-controller/settings-controller';
import { replaceWorkbookModel, resetWorkbookModel } from '../../api/generated/endpoints/workbook-controller/workbook-controller';
import type { CardModel, MemberModel } from '../../api/generated/model';
import { apiFetchFile } from '../../api/http';
import { reportError } from '../../app/queryClient';
import { Field } from '../../shared/components/Field';
import { downloadFile } from '../../shared/desktop';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';
import { type SettingsMap, useSettingsMap } from './GeneralSettings';

const KEYS = ['workbook-member-sheets', 'workbook-months-before', 'workbook-months-after', 'workbook-formula-mode', 'workbook-suffix', 'workbook-card-order'];

const ids = (value: string | undefined) => (value ? value.split(',').map(Number) : []);

/**
 * Monthly spreadsheet settings: member sheets and neighbouring months, formula mode, file suffix, card order and
 * the model file (XLS-01, XLS-03, XLS-06, XLS-09, XLS-14).
 *
 * @returns the section
 */
export function WorkbookSettings() {
  const { t } = useTranslation();
  const saved = useSaved();
  const settings = useSettingsMap();
  const { data: memberPage } = useMembers({ size: 200, sort: ['code,asc'] });
  const { data: cardPage } = useCards({ size: 200, sort: ['nickname,asc'] });
  const members = embedded<MemberModel>(memberPage, 'members').filter((m) => m.active);
  const cards = embedded<CardModel>(cardPage, 'cards').filter((c) => c.primaryCardId == null);
  const [changes, setChanges] = useState<SettingsMap>({});
  const draft: SettingsMap = { ...settings, ...changes };
  const set = (key: string, value: string) => setChanges((d) => ({ ...d, [key]: value }));
  const sheets = ids(draft['workbook-member-sheets']);
  const order = ids(draft['workbook-card-order']);
  const orderedCards = [...cards].sort((a, b) => {
    const ia = order.indexOf(a.id ?? 0);
    const ib = order.indexOf(b.id ?? 0);
    return (ia < 0 ? Number.MAX_SAFE_INTEGER : ia) - (ib < 0 ? Number.MAX_SAFE_INTEGER : ib);
  });
  const move = (index: number, delta: number) => {
    const next = orderedCards.map((c) => c.id ?? 0);
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item!);
    set('workbook-card-order', next.join(','));
  };

  return (
    <form
      className="panel form-grid"
      aria-labelledby="workbook-settings-title"
      onSubmit={async (event) => {
        event.preventDefault();
        try {
          await changeSettings(Object.fromEntries(KEYS.filter((k) => draft[k] !== undefined).map((k) => [k, draft[k]!])));
          setChanges({});
          await saved();
        } catch (error) {
          reportError(error);
        }
      }}
    >
      <h2 id="workbook-settings-title" className="col-span-full text-lg font-semibold">
        {t('workbook.settings')}
      </h2>
      <fieldset className="col-span-2 flex flex-col gap-1 self-start">
        <legend className="text-sm text-(--color-text-muted)">{t('workbook.memberSheets')}</legend>
        {members.map((m) => (
          <label key={m.id} className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={sheets.includes(m.id ?? 0)}
              onChange={(e) =>
                set('workbook-member-sheets', (e.target.checked ? [...sheets, m.id ?? 0] : sheets.filter((x) => x !== m.id)).join(','))
              }
            />
            {m.code} — {m.name}
          </label>
        ))}
      </fieldset>
      <fieldset className="col-span-2 flex flex-col gap-1 self-start">
        <legend className="text-sm text-(--color-text-muted)">{t('workbook.cardOrder')}</legend>
        {orderedCards.map((c, i) => (
          <div key={c.id} className="flex items-center gap-2 text-sm">
            <span className="flex-1">{c.nickname}</span>
            <button type="button" className="icon-button" aria-label={t('templates.moveUp')} disabled={i === 0} onClick={() => move(i, -1)}>
              <ArrowUp size={14} aria-hidden />
            </button>
            <button type="button" className="icon-button" aria-label={t('templates.moveDown')} disabled={i === orderedCards.length - 1} onClick={() => move(i, 1)}>
              <ArrowDown size={14} aria-hidden />
            </button>
          </div>
        ))}
      </fieldset>
      <Field label={t('workbook.monthsBefore')} htmlFor="workbook-before">
        <input id="workbook-before" type="number" min={0} max={12} className="input" value={draft['workbook-months-before'] ?? ''} onChange={(e) => set('workbook-months-before', e.target.value)} />
      </Field>
      <Field label={t('workbook.monthsAfter')} htmlFor="workbook-after">
        <input id="workbook-after" type="number" min={0} max={12} className="input" value={draft['workbook-months-after'] ?? ''} onChange={(e) => set('workbook-months-after', e.target.value)} />
      </Field>
      <Field label={t('workbook.mode')} htmlFor="workbook-settings-mode">
        <select id="workbook-settings-mode" className="input" value={draft['workbook-formula-mode'] ?? ''} onChange={(e) => set('workbook-formula-mode', e.target.value)}>
          <option value="">{t('workbook.modeSystem')}</option>
          <option value="EXCEL_365">{t('workbook.modes.EXCEL_365')}</option>
          <option value="COMPATIBLE">{t('workbook.modes.COMPATIBLE')}</option>
        </select>
      </Field>
      <Field label={t('workbook.suffix')} htmlFor="workbook-suffix">
        <input id="workbook-suffix" className="input" maxLength={20} value={draft['workbook-suffix'] ?? ''} onChange={(e) => set('workbook-suffix', e.target.value)} />
      </Field>
      <div className="col-span-full flex flex-col gap-2">
        <span className="text-sm font-semibold">{t('workbook.model')}</span>
        <p className="text-xs text-(--color-text-muted)">{t('workbook.modelHint')}</p>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            className="button"
            onClick={async () => {
              try {
                const file = await apiFetchFile('/api/workbook-model');
                downloadFile(file.blob, file.fileName ?? 'monthly-workbook-model.xlsx');
              } catch (error) {
                reportError(error);
              }
            }}
          >
            {t('workbook.downloadModel')}
          </button>
          <label className="button cursor-pointer" htmlFor="workbook-model-upload">
            {t('workbook.uploadModel')}
          </label>
          <input
            id="workbook-model-upload"
            type="file"
            accept=".xlsx"
            className="sr-only"
            onChange={async (e) => {
              const file = e.target.files?.[0];
              if (!file) return;
              try {
                await replaceWorkbookModel({ file });
                await saved();
              } catch (error) {
                reportError(error);
              } finally {
                e.target.value = '';
              }
            }}
          />
          <button
            type="button"
            className="button"
            onClick={async () => {
              try {
                await resetWorkbookModel();
                await saved();
              } catch (error) {
                reportError(error);
              }
            }}
          >
            {t('workbook.resetModel')}
          </button>
        </div>
      </div>
      <div className="col-span-full flex justify-end">
        <button type="submit" className="button button-primary">
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

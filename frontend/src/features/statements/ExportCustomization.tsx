import { useTranslation } from 'react-i18next';
import type { MemberStatementHideItem, TemplateSettingsBlocksItem } from '../../api/generated/model';
import { BlockOrderList } from '../../shared/components/BlockOrderList';
import { Field } from '../../shared/components/Field';

/** Columns of the line tables an export can leave out. */
export const EXPORT_COLUMNS: readonly MemberStatementHideItem[] = ['DATE', 'INSTALLMENT', 'SHARE'];

/** Changes one export makes to its template (IMP-60). */
export interface ExportCustomizationValue {
  /** Header text, or empty for the template's. */
  header: string;
  /** Footer text, or empty for the template's. */
  footer: string;
  /** Blocks shown, in order, or `null` for the template's. */
  blocks: TemplateSettingsBlocksItem[] | null;
  /** Columns left out of the line tables. */
  hide: MemberStatementHideItem[];
}

/** No change: the export looks like its template. */
export const NO_CUSTOMIZATION: ExportCustomizationValue = { header: '', footer: '', blocks: null, hide: [] };

/**
 * Tells whether a customization changes anything.
 *
 * @param value - customization
 * @returns `true` when the export differs from its template
 */
export function isCustomized(value: ExportCustomizationValue): boolean {
  return Boolean(value.header.trim() || value.footer.trim() || value.blocks || value.hide.length > 0);
}

/** Props of {@link ExportCustomization}. */
export interface ExportCustomizationProps {
  /** Current changes. */
  value: ExportCustomizationValue;
  /** Called with the new changes. */
  onChange: (value: ExportCustomizationValue) => void;
  /** Blocks of the template in use, the starting point when the blocks are chosen here. */
  templateBlocks: TemplateSettingsBlocksItem[];
}

/**
 * Fine control of one export without changing the template (IMP-60): header and footer texts, which blocks are shown
 * and in what order, and which columns the line tables keep.
 *
 * @param props - {@link ExportCustomizationProps}
 * @returns the section
 */
export function ExportCustomization({ value, onChange, templateBlocks }: ExportCustomizationProps) {
  const { t } = useTranslation();
  const set = (change: Partial<ExportCustomizationValue>) => onChange({ ...value, ...change });
  const customized = isCustomized(value);

  return (
    <details className="panel" open={customized || undefined}>
      <summary className="cursor-pointer font-semibold">
        {t('statements.custom.title')}
        {customized && <span className="badge badge-warning ml-2">{t('statements.custom.changed')}</span>}
      </summary>
      <div className="mt-2 flex flex-col gap-3">
        <p className="text-xs text-(--color-text-muted)">{t('statements.custom.hint')}</p>
        <Field label={t('statements.custom.header')} htmlFor="export-header">
          <input
            id="export-header"
            className="input"
            maxLength={200}
            placeholder={t('statements.custom.fromTemplate')}
            value={value.header}
            onChange={(e) => set({ header: e.target.value })}
          />
        </Field>
        <Field label={t('statements.custom.footer')} htmlFor="export-footer">
          <input
            id="export-footer"
            className="input"
            maxLength={200}
            placeholder={t('statements.custom.fromTemplate')}
            value={value.footer}
            onChange={(e) => set({ footer: e.target.value })}
          />
        </Field>
        <fieldset className="flex flex-col gap-1">
          <legend className="text-sm font-medium">{t('statements.custom.blocks')}</legend>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={value.blocks !== null}
              onChange={(e) => set({ blocks: e.target.checked ? [...templateBlocks] : null })}
            />
            {t('statements.custom.chooseBlocks')}
          </label>
          {value.blocks && <BlockOrderList blocks={value.blocks} onChange={(blocks) => set({ blocks })} requireOne />}
        </fieldset>
        <fieldset className="flex flex-col gap-1">
          <legend className="text-sm font-medium">{t('statements.custom.columns')}</legend>
          {EXPORT_COLUMNS.map((column) => (
            <label key={column} className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={!value.hide.includes(column)}
                onChange={(e) =>
                  set({ hide: e.target.checked ? value.hide.filter((c) => c !== column) : [...value.hide, column] })
                }
              />
              {t(`statements.custom.columnNames.${column}`)}
            </label>
          ))}
        </fieldset>
        {customized && (
          <button type="button" className="button self-start" onClick={() => onChange(NO_CUSTOMIZATION)}>
            {t('statements.custom.reset')}
          </button>
        )}
      </div>
    </details>
  );
}

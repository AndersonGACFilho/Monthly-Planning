import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useTemplateLayoutReference } from '../../api/generated/endpoints/template-controller/template-controller';

/** Props of {@link LayoutReferencePanel}. */
export interface LayoutReferencePanelProps {
  /** Inserts a placeholder or a variable, such as `{{block.cards}}`, into the layout. */
  onInsertPlaceholder: (placeholder: string) => void;
  /** Inserts an empty rule for a selector, such as `#block-cards`, into the extra CSS. */
  onInsertSelector: (selector: string) => void;
}

/**
 * Reference of the full layout (TPL-07): the placeholders of the statement parts, the message variables and every
 * class and id of the statement with what it marks, filtered by a search; a click inserts it where it is used.
 *
 * @param props - {@link LayoutReferencePanelProps}
 * @returns the panel
 */
export function LayoutReferencePanel({ onInsertPlaceholder, onInsertSelector }: LayoutReferencePanelProps) {
  const { t } = useTranslation();
  const { data: reference } = useTemplateLayoutReference();
  const [query, setQuery] = useState('');
  const needle = query.trim().toLowerCase();
  const selectors = (reference?.selectors ?? []).filter(
    (s) => !needle || `${s.selector} ${s.description}`.toLowerCase().includes(needle),
  );
  const chip = (name: string) => (
    <li key={name}>
      <button
        type="button"
        className="rounded border border-(--color-border) px-1.5 py-0.5 font-mono text-xs hover:bg-(--color-surface-muted)"
        title={t('templates.layout.insert')}
        onClick={() => onInsertPlaceholder(`{{${name}}}`)}
      >
        {`{{${name}}}`}
      </button>
    </li>
  );

  return (
    <section className="flex flex-col gap-2" aria-labelledby="layout-reference-title">
      <h3 id="layout-reference-title" className="font-semibold">
        {t('templates.layout.reference')}
      </h3>
      <p className="text-xs text-(--color-text-muted)">{t('templates.layout.placeholders')}</p>
      <ul className="flex flex-wrap gap-1" aria-label={t('templates.layout.placeholders')}>
        {(reference?.placeholders ?? []).map(chip)}
      </ul>
      <p className="text-xs text-(--color-text-muted)">{t('templates.layout.variables')}</p>
      <ul className="flex flex-wrap gap-1" aria-label={t('templates.layout.variables')}>
        {(reference?.variables ?? []).map(chip)}
      </ul>
      <div className="flex flex-wrap items-end justify-between gap-2">
        <p className="text-xs text-(--color-text-muted)">
          {t('templates.layout.selectors', { count: reference?.selectors?.length ?? 0 })}
        </p>
        <input
          type="search"
          className="input w-48"
          placeholder={t('common.search')}
          aria-label={t('templates.layout.searchSelectors')}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>
      <div className="max-h-64 overflow-auto rounded border border-(--color-border)">
        <table className="table text-xs">
          <thead>
            <tr>
              <th scope="col">{t('templates.layout.selector')}</th>
              <th scope="col">{t('templates.layout.marks')}</th>
              <th scope="col">
                <span className="sr-only">{t('common.actions')}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {selectors.map((s) => (
              <tr key={s.selector}>
                <th scope="row" className="font-mono font-normal">
                  {s.selector}
                </th>
                <td>{s.description}</td>
                <td className="text-right">
                  <button
                    type="button"
                    className="button whitespace-nowrap"
                    aria-label={t('templates.layout.useInCssFor', { selector: s.selector })}
                    onClick={() => onInsertSelector(s.selector ?? '')}
                  >
                    {t('templates.layout.useInCss')}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

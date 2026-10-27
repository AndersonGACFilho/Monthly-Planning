import { ChevronDown, ChevronRight } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { editImportLine } from '../../api/generated/endpoints/import-controller/import-controller';
import type { ImportLineModel, ImportModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatDate } from '../../shared/format';
import { linkOf } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';
import { STATUS_ORDER, WARNING_KEYS } from './importText';

/** Props of {@link ImportPreview}. */
export interface ImportPreviewProps {
  /** Batch with its lines. */
  batch: ImportModel;
  /** Called with the batch after any change. */
  onChange: (batch: ImportModel | null) => void;
}

/**
 * Preview of an import: counts, totals, warnings and lines by status, divergent lines expanded with a
 * decision to make; confirm, cancel and undo come from the batch links (RF-62, RF-68 to RF-72, UI-06).
 *
 * @param props - {@link ImportPreviewProps}
 * @returns the preview
 */
export function ImportPreview({ batch, onChange }: ImportPreviewProps) {
  const { t } = useTranslation();
  const confirm = useAction(batch, 'confirm');
  const cancel = useAction(batch, 'cancel');
  const undo = useAction(batch, 'undo');
  const [acknowledged, setAcknowledged] = useState(false);
  const warnings = batch.warnings ?? [];
  const lines = (batch.lines ?? []).filter((l) => !l.futureCheck);
  const future = (batch.lines ?? []).filter((l) => l.futureCheck);
  const undecided = lines.some((l) => l.decision === 'UNDECIDED');
  const inPreview = batch.status === 'PREVIEW';

  const decide = async (line: ImportLineModel, decision: ImportLineModel['decision']) => {
    try {
      onChange(await editImportLine(batch.id!, line.id!, { decision }));
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <section className="flex flex-col gap-4" aria-labelledby="import-preview-title">
      <header className="panel flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 id="import-preview-title" className="text-lg font-semibold">
            {batch.fileName}
          </h2>
          <p className="text-sm text-(--color-text-muted)">
            {t(`import.status.${batch.status}`)} · {t('period.label')} {batch.billingPeriod} · {t('import.due')}{' '}
            {formatDate(batch.dueDate)} · {batch.openBill ? t('import.openBill') : t('import.closedBill')}
          </p>
          <ul className="mt-2 flex flex-wrap gap-3 text-sm">
            {STATUS_ORDER.map((status) => (
              <li key={status}>
                {t(`import.line.${status}`)}: <strong>{countOf(batch, status)}</strong>
              </li>
            ))}
          </ul>
        </div>
        <dl className="grid grid-cols-2 gap-x-4 text-sm">
          <dt>{t('import.fileTotal')}</dt>
          <dd className="text-right">
            <MoneyText value={batch.entriesTotal ?? batch.fileTotal} />
          </dd>
          <dt>{t('import.computedTotal')}</dt>
          <dd className="text-right">
            <MoneyText value={batch.computedTotal} />
          </dd>
          <dt>{t('import.difference')}</dt>
          <dd className={`text-right ${Number(batch.totalDifference) !== 0 ? 'font-semibold text-red-600' : ''}`}>
            <MoneyText value={batch.totalDifference} />
          </dd>
        </dl>
      </header>

      {warnings.length > 0 && (
        <div role="alert" className="panel border-amber-500">
          <ul className="list-disc pl-5 text-sm">
            {warnings.map((w) => (
              <li key={w}>{t(WARNING_KEYS[w] ?? w)}</li>
            ))}
          </ul>
          {inPreview && (
            <label className="mt-2 flex items-center gap-2 text-sm">
              <input type="checkbox" checked={acknowledged} onChange={(e) => setAcknowledged(e.target.checked)} />
              {t('import.acknowledge')}
            </label>
          )}
        </div>
      )}

      {STATUS_ORDER.map((status) => (
        <LineGroup
          key={status}
          status={status}
          lines={lines.filter((l) => l.status === status)}
          expanded={status === 'DIVERGENT'}
          editable={inPreview}
          onDecide={decide}
        />
      ))}
      {future.length > 0 && <LineGroup status="FUTURE" lines={future} expanded={false} editable={false} onDecide={decide} />}

      <footer className="flex flex-wrap justify-end gap-2">
        {batch.status === 'CONFIRMED' && linkOf(batch, 'review-queue') && (
          <Link className="button" to={`/review-queue?batch=${batch.id}`}>
            {t('import.openReviewQueue')}
          </Link>
        )}
        {cancel.available && (
          <button type="button" className="button" onClick={() => cancel.mutate({ method: 'DELETE' }, { onSuccess: () => onChange(null) })}>
            {t('import.cancel')}
          </button>
        )}
        {undo.available && (
          <button
            type="button"
            className="button button-danger"
            onClick={() => window.confirm(t('import.confirmUndo')) && undo.mutate({ method: 'POST' }, { onSuccess: () => onChange(null) })}
          >
            {t('import.undo')}
          </button>
        )}
        {confirm.available && (
          <button
            type="button"
            className="button button-primary"
            disabled={undecided || (warnings.length > 0 && !acknowledged) || confirm.isPending}
            title={undecided ? t('import.decideDivergent') : undefined}
            onClick={() =>
              confirm.mutate(
                { method: 'POST', body: { acknowledgeTotals: acknowledged } },
                { onSuccess: (result) => onChange(result as ImportModel) },
              )
            }
          >
            {t('import.confirm')}
          </button>
        )}
      </footer>
    </section>
  );
}

function countOf(batch: ImportModel, status: (typeof STATUS_ORDER)[number]): number {
  return {
    NEW: batch.newCount,
    DUPLICATE: batch.duplicateCount,
    MATCHED: batch.matchedCount,
    DIVERGENT: batch.divergentCount,
  }[status] ?? 0;
}

interface LineGroupProps {
  status: string;
  lines: ImportLineModel[];
  expanded: boolean;
  editable: boolean;
  onDecide: (line: ImportLineModel, decision: ImportLineModel['decision']) => void;
}

function LineGroup({ status, lines, expanded, editable, onDecide }: LineGroupProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(expanded);
  if (lines.length === 0) return null;
  return (
    <section className="panel p-0">
      <h3>
        <button type="button" className="flex w-full items-center gap-2 p-3 text-left font-medium" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? <ChevronDown size={16} aria-hidden /> : <ChevronRight size={16} aria-hidden />}
          {t(`import.line.${status}`)} ({lines.length})
        </button>
      </h3>
      {open && (
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-y border-(--color-border) text-left text-(--color-text-muted)">
              <th className="px-2 py-1">{t('entry.date')}</th>
              <th className="px-2 py-1">{t('entry.description')}</th>
              <th className="px-2 py-1">{t('entry.installment')}</th>
              <th className="hidden px-2 py-1 wide:table-cell">{t('entry.card')}</th>
              <th className="px-2 py-1 text-right">{t('entry.amount')}</th>
              <th className="px-2 py-1">{t('import.decision')}</th>
            </tr>
          </thead>
          <tbody>
            {lines.map((line) => (
              <tr key={line.id} className="border-b border-(--color-border)">
                <td className="px-2 py-1">{formatDate(line.purchaseDate)}</td>
                <td className="px-2 py-1">
                  {line.description}
                  {line.unknownFinal && <span className="ml-2 text-xs text-amber-600">{t('import.unknownFinal')}</span>}
                  {line.note && <span className="block text-xs text-(--color-text-muted)">{line.note}</span>}
                </td>
                <td className="px-2 py-1">{line.installment}</td>
                <td className="hidden px-2 py-1 wide:table-cell">{line.cardLastDigits}</td>
                <td className="px-2 py-1 text-right">
                  <MoneyText value={line.amount} />
                </td>
                <td className="px-2 py-1">
                  {editable && status !== 'FUTURE' ? (
                    <select
                      className="input"
                      aria-label={t('import.decision')}
                      value={line.decision}
                      onChange={(e) => onDecide(line, e.target.value as ImportLineModel['decision'])}
                    >
                      {status === 'DIVERGENT' && <option value="UNDECIDED">{t('import.decisions.UNDECIDED')}</option>}
                      <option value="IMPORT">{t(status === 'DIVERGENT' ? 'import.decisions.IMPORT_AS_NEW' : 'import.decisions.IMPORT')}</option>
                      {status === 'DIVERGENT' && <option value="UPDATE_EXISTING">{t('import.decisions.UPDATE_EXISTING')}</option>}
                      <option value="IGNORE">{t('import.decisions.IGNORE')}</option>
                    </select>
                  ) : (
                    t(`import.decisions.${line.decision}`)
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

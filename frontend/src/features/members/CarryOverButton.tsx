import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { carryOver, undoCarryOver } from '../../api/generated/endpoints/carry-over-controller/carry-over-controller';
import type { MemberRow } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { Modal } from '../../shared/components/Modal';
import { formatMoney, formatPeriod, shiftPeriod } from '../../shared/format';
import { useMemberMap } from '../../shared/hooks/useLookups';
import { useSaved } from '../../shared/hooks/useSaved';

/** Third parties who owe something, or who already carried something over. */
function carryRows(rows: MemberRow[]) {
  return rows.filter((row) => !row.main && ((row.unpaid ?? 0) > 0 || (row.carriedOut ?? 0) > 0));
}

/** Tells whether the unpaid amount can be carried now: nothing carried yet, or the month changed since. */
function canCarry(row: MemberRow) {
  return (row.unpaid ?? 0) > 0 && row.unpaid !== row.carriedOut;
}

/**
 * Carries what each third party still owes after the incomes received into the next month, and undoes it (RF-28).
 * The entries stay in their own month, so each month keeps its total in the statements and in the spreadsheet.
 *
 * @param props - billing period and the totals by member
 * @returns the button and, when open, the dialog
 */
export function CarryOverButton({ period, rows }: { period: string; rows: MemberRow[] }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const candidates = carryRows(rows);
  return (
    <>
      <button type="button" className="button" disabled={candidates.length === 0} onClick={() => setOpen(true)}>
        {t('carryOver.button')}
      </button>
      {open && <CarryOverDialog period={period} rows={candidates} onClose={() => setOpen(false)} />}
    </>
  );
}

function CarryOverDialog({ period, rows, onClose }: { period: string; rows: MemberRow[]; onClose: () => void }) {
  const { t } = useTranslation();
  const members = useMemberMap();
  const saved = useSaved();
  const next = formatPeriod(shiftPeriod(period, 1));
  const [selected, setSelected] = useState(() => new Set(rows.filter(canCarry).map((row) => row.memberId ?? 0)));
  const toggle = (memberId: number) =>
    setSelected((current) => {
      const changed = new Set(current);
      if (!changed.delete(memberId)) changed.add(memberId);
      return changed;
    });
  const nameOf = (row: MemberRow) => {
    const member = members.get(row.memberId ?? 0);
    return member ? `${member.code} — ${member.name}` : String(row.memberId);
  };
  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      await saved();
    } catch (error) {
      reportError(error);
    }
  };
  const submit = () =>
    run(async () => {
      await carryOver(period, { memberIds: [...selected] });
      onClose();
    });
  const undo = (row: MemberRow) => {
    if (window.confirm(t('carryOver.confirmUndo', { member: nameOf(row), period: next }))) {
      void run(() => undoCarryOver(period, row.memberId ?? 0));
    }
  };

  return (
    <Modal title={t('carryOver.title', { period: next })} onClose={onClose}>
      <div className="flex flex-col gap-3 p-2">
        <p className="text-sm text-(--color-text-muted)">{t('carryOver.hint', { period: next })}</p>
        {rows.length === 0 ? (
          <p>{t('carryOver.nothing')}</p>
        ) : (
          <ul className="flex flex-col divide-y divide-(--color-border)">
            {rows.map((row) => {
              const memberId = row.memberId ?? 0;
              const carried = (row.carriedOut ?? 0) > 0;
              return (
                <li key={memberId} className="flex flex-wrap items-center gap-3 py-2">
                  <label className="flex flex-1 items-center gap-2">
                    <input
                      type="checkbox"
                      checked={selected.has(memberId)}
                      disabled={!canCarry(row)}
                      onChange={() => toggle(memberId)}
                    />
                    <span>{nameOf(row)}</span>
                  </label>
                  <span className="text-sm">{t('carryOver.unpaid', { amount: formatMoney(Math.max(row.unpaid ?? 0, 0)) })}</span>
                  {carried && (
                    <>
                      <span className="text-sm text-(--color-text-muted)">
                        {t('carryOver.carried', { amount: formatMoney(row.carriedOut) })}
                      </span>
                      {row.unpaid !== row.carriedOut && <span className="text-sm text-amber-700 dark:text-amber-400">{t('carryOver.outdated')}</span>}
                      <button type="button" className="button" onClick={() => undo(row)}>
                        {t('carryOver.undo', { member: nameOf(row) })}
                      </button>
                    </>
                  )}
                </li>
              );
            })}
          </ul>
        )}
        <div className="flex justify-end gap-2">
          <button type="button" className="button" onClick={onClose}>
            {t('action.cancel')}
          </button>
          <button type="button" className="button button-primary" disabled={selected.size === 0} onClick={() => void submit()}>
            {t('carryOver.submit')}
          </button>
        </div>
      </div>
    </Modal>
  );
}

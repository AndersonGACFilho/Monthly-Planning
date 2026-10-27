import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useBillEntry } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import {
  chooseOccurrenceLink,
  usePendingOccurrenceLinks,
} from '../../api/generated/endpoints/occurrence-controller/occurrence-controller';
import type { OccurrenceModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatDate, formatMoney, formatPeriod } from '../../shared/format';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';

/**
 * Card-paid fixed expenses with more than one candidate card entry; the user picks the right one (RN-30).
 *
 * @returns the section, or nothing when no expense waits
 */
export function PendingLinks() {
  const { t } = useTranslation();
  const { data } = usePendingOccurrenceLinks();
  const pending = embedded<OccurrenceModel>(data, 'occurrences');
  if (pending.length === 0) return null;
  return (
    <section className="panel flex flex-col gap-2" aria-labelledby="pending-links-title">
      <h2 id="pending-links-title" className="font-semibold">
        {t('review.pendingLinks')}
      </h2>
      <ul className="flex flex-col divide-y divide-(--color-border)">
        {pending.map((occurrence) => (
          <PendingLink key={occurrence.id} occurrence={occurrence} />
        ))}
      </ul>
    </section>
  );
}

function PendingLink({ occurrence }: { occurrence: OccurrenceModel }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const candidates = occurrence.linkCandidates ?? [];
  const [choice, setChoice] = useState<number | undefined>(candidates[0]);
  const selectId = `link-${occurrence.id}`;
  return (
    <li className="flex flex-wrap items-center gap-3 py-2">
      <span className="min-w-48">
        <span className="block font-medium">{occurrence.description}</span>
        <span className="block text-xs text-(--color-text-muted)">
          {formatPeriod(occurrence.billingPeriod ?? '')} · <MoneyText value={occurrence.amount} />
        </span>
      </span>
      <label htmlFor={selectId} className="sr-only">
        {t('review.chooseLink')}
      </label>
      <select id={selectId} className="input w-auto" value={choice ?? ''} onChange={(e) => setChoice(Number(e.target.value))}>
        {candidates.map((entryId) => (
          <CandidateOption key={entryId} entryId={entryId} />
        ))}
      </select>
      <button
        type="button"
        className="button button-primary"
        disabled={choice === undefined}
        onClick={async () => {
          if (choice === undefined || occurrence.id === undefined) return;
          try {
            await chooseOccurrenceLink(occurrence.id, { entryId: choice });
            await saved();
          } catch (error) {
            reportError(error);
          }
        }}
      >
        {t('review.chooseLink')}
      </button>
    </li>
  );
}

function CandidateOption({ entryId }: { entryId: number }) {
  const { t } = useTranslation();
  const { data: entry } = useBillEntry(entryId);
  return (
    <option value={entryId}>
      {entry
        ? t('review.linkCandidate', {
            date: formatDate(entry.entryDate),
            description: entry.displayDescription,
            amount: formatMoney(entry.amount),
          })
        : entryId}
    </option>
  );
}

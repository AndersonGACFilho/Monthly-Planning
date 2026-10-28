import { useQuery } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { apiFetch } from '../../api/http';
import type { BillEntryModel } from '../../api/generated/model';
import { MoneyText } from '../../shared/components/MoneyText';
import { DeleteButton } from '../../shared/components/ResourceActions';
import { formatDate } from '../../shared/format';
import { embedded, linkOf } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';

/** Props of {@link EntryDetails}. */
export interface EntryDetailsProps {
  /** Selected entry. */
  entry: BillEntryModel;
  /** Opens the form that replaces a manual purchase. */
  onEdit: () => void;
}

/**
 * Details panel of the selected entry, shown on wide windows: original description, purchase installments,
 * status and note (SPEC section 14, UI-05); a manual entry can be edited or deleted (RF-44).
 *
 * @param props - {@link EntryDetailsProps}
 * @returns the panel
 */
export function EntryDetails({ entry, onEdit }: EntryDetailsProps) {
  const { t } = useTranslation();
  const purchaseHref = linkOf(entry, 'purchase');
  const { data } = useQuery({
    queryKey: ['installments', purchaseHref],
    queryFn: () => apiFetch<{ _embedded?: { billEntries?: BillEntryModel[] } }>(`${purchaseHref}/installments`),
    enabled: Boolean(purchaseHref),
  });
  const confirm = useAction(entry, 'confirm');
  const installments = embedded<BillEntryModel>(data, 'billEntries');

  return (
    <aside className="panel hidden w-80 shrink-0 flex-col gap-2 self-start text-sm wide:flex" aria-label={t('entries.details')}>
      <div className="flex items-start justify-between gap-2">
        <h2 className="font-semibold">{entry.displayDescription}</h2>
        <span className="flex gap-1">
          {linkOf(entry, 'replace-purchase') && (
            <button type="button" className="icon-button" title={t('action.edit')} aria-label={t('action.edit')} onClick={onEdit}>
              <Pencil size={16} aria-hidden />
            </button>
          )}
          <DeleteButton
            resource={entry}
            confirmMessage={installments.length > 1 ? t('entries.confirmDeleteInstallments', { count: installments.length }) : undefined}
          />
        </span>
      </div>
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1">
        <dt className="text-(--color-text-muted)">{t('entries.original')}</dt>
        <dd>{entry.originalDescription}</dd>
        <dt className="text-(--color-text-muted)">{t('entries.purchaseDate')}</dt>
        <dd>{formatDate(entry.purchaseDate)}</dd>
        <dt className="text-(--color-text-muted)">{t('common.status')}</dt>
        <dd>{t(`entries.status.${entry.status}`)}</dd>
        {entry.refundStatus !== 'NONE' && (
          <>
            <dt className="text-(--color-text-muted)">{t('entries.refund')}</dt>
            <dd>{t(`entries.refundStatus.${entry.refundStatus}`)}</dd>
          </>
        )}
        {entry.note && (
          <>
            <dt className="text-(--color-text-muted)">{t('review.note')}</dt>
            <dd>{entry.note}</dd>
          </>
        )}
      </dl>
      {installments.length > 1 && (
        <>
          <h3 className="mt-2 font-medium">{t('entries.installments')}</h3>
          <ul className="flex flex-col gap-1">
            {installments.map((i) => (
              <li key={i.id} className="flex justify-between gap-2">
                <span>
                  {i.installment} · {i.billingPeriod}
                </span>
                <span className="text-(--color-text-muted)">{t(`entries.status.${i.status}`)}</span>
                <MoneyText value={i.amount} />
              </li>
            ))}
          </ul>
        </>
      )}
      {confirm.available && (
        <button type="button" className="button self-start" onClick={() => confirm.mutate({ method: 'POST' })}>
          {t('entries.confirmProjected')}
        </button>
      )}
    </aside>
  );
}

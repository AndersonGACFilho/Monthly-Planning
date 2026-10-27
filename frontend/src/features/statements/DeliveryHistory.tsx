import { useTranslation } from 'react-i18next';
import { useStatementDeliveries } from '../../api/generated/endpoints/statement-delivery-controller/statement-delivery-controller';
import type { DeliveryModel } from '../../api/generated/model';
import { formatPeriod } from '../../shared/format';
import { embedded } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';
import { useMemberMap } from '../../shared/hooks/useLookups';

/**
 * Delivery history: member, period, channel, date, status, template version and language, with the outdated
 * flag, resend and mark as sent (RF-92, RF-93, I18N-12).
 *
 * @param props - billing period shown, or every period
 * @returns the table
 */
export function DeliveryHistory({ period }: { period?: string }) {
  const { t, i18n } = useTranslation();
  const { data } = useStatementDeliveries(period ? { billingPeriod: period } : undefined);
  const members = useMemberMap();
  const rows = embedded<DeliveryModel>(data, 'deliveries');
  const date = new Intl.DateTimeFormat(i18n.language, { dateStyle: 'short', timeStyle: 'short' });
  return (
    <section className="flex flex-col gap-2" aria-labelledby="history-title">
      <h2 id="history-title" className="text-lg font-semibold">
        {t('statements.history')}
      </h2>
      <div className="overflow-x-auto">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">{t('statements.member')}</th>
              <th scope="col">{t('statements.period')}</th>
              <th scope="col">{t('statements.channel')}</th>
              <th scope="col">{t('statements.date')}</th>
              <th scope="col">{t('common.status')}</th>
              <th scope="col" className="hidden wide:table-cell">
                {t('statements.templateVersion')}
              </th>
              <th scope="col" className="hidden wide:table-cell">
                {t('statements.language')}
              </th>
              <th scope="col">
                <span className="sr-only">{t('common.actions')}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan={8} className="text-(--color-text-muted)">
                  {t('statements.noHistory')}
                </td>
              </tr>
            )}
            {rows.map((row) => (
              <HistoryRow key={row.id} row={row} member={members.get(row.memberId ?? 0)?.code} format={date} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function HistoryRow({ row, member, format }: { row: DeliveryModel; member?: string; format: Intl.DateTimeFormat }) {
  const { t } = useTranslation();
  const resend = useAction(row, 'resend');
  const markSent = useAction(row, 'mark-sent');
  return (
    <tr>
      <td>{member}</td>
      <td>{formatPeriod(row.billingPeriod ?? '')}</td>
      <td>{t(`statements.channels.${row.channel ?? 'FOLDER'}`)}</td>
      <td>{row.createdAt ? format.format(new Date(row.createdAt)) : ''}</td>
      <td>
        <span className={`badge ${row.status === 'SENT' ? 'badge-success' : row.status === 'FAILED' ? 'text-red-700' : 'badge-warning'}`}>
          {t(`statements.status.${row.status ?? 'FAILED'}`)}
        </span>
        {row.outdated && (
          <span className="badge badge-warning ml-1" title={t('statements.outdatedHint')}>
            {t('statements.outdated')}
          </span>
        )}
      </td>
      <td className="hidden wide:table-cell">
        #{row.templateId} ({t('templates.version', { number: row.templateVersion })})
      </td>
      <td className="hidden wide:table-cell">{row.language}</td>
      <td className="whitespace-nowrap">
        {markSent.available && (
          <button type="button" className="button" onClick={() => markSent.mutate({ method: 'POST' })}>
            {t('statements.markSent')}
          </button>
        )}
        {resend.available && (
          <button type="button" className="button ml-1" onClick={() => resend.mutate({ method: 'POST' })}>
            {t('statements.resend')}
          </button>
        )}
      </td>
    </tr>
  );
}

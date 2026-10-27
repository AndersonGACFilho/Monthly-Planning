import { CreditCard, FileUp, Wallet } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useCardBills } from '../../api/generated/endpoints/card-bill-controller/card-bill-controller';
import type { CardBillModel } from '../../api/generated/model';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatDate, formatMoney } from '../../shared/format';
import { embedded, linkOf } from '../../shared/hal';
import { useAction } from '../../shared/hooks/useAction';

/**
 * One card per credit card with the bill total, closing and due dates, status and the split into one-off,
 * installments and subscriptions; clicking opens the card entries of the period (RF-36 to RF-38).
 *
 * @param props - selected billing period
 * @returns the section
 */
export function CardBillCards({ period }: { period: string }) {
  const { t } = useTranslation();
  const { data } = useCardBills({ billingPeriod: period });
  const bills = embedded<CardBillModel>(data, 'cardBills');
  return (
    <section className="flex flex-col gap-3" aria-labelledby="card-bills-title">
      <h2 id="card-bills-title" className="text-lg font-semibold">
        {t('cardBill.title')}
      </h2>
      {bills.length === 0 && <p className="text-(--color-text-muted)">{t('common.empty')}</p>}
      <div className="grid gap-3 wide:grid-cols-3">
        {bills.map((bill) => (
          <CardBillCard key={bill.cardId} bill={bill} period={period} />
        ))}
      </div>
    </section>
  );
}

function CardBillCard({ bill, period }: { bill: CardBillModel; period: string }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const pay = useAction(bill, 'mark-paid');
  const open = () => navigate(`/entries?billingPeriod=${period}&card=${bill.cardId}`);
  return (
    <article className="panel flex flex-col gap-2" aria-label={bill.nickname}>
      <button
        type="button"
        className="flex flex-col gap-1 text-left"
        onClick={open}
        title={t('cardBill.open', { card: bill.nickname })}
        aria-label={t('cardBill.open', { card: bill.nickname })}
      >
        <span className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-2 font-semibold">
            <CreditCard size={16} aria-hidden /> {bill.nickname}
            <span className="text-xs font-normal text-(--color-text-muted)">{bill.lastDigits?.join(', ')}</span>
          </span>
          <span className={`badge ${bill.status === 'PAID' ? 'badge-success' : bill.status === 'CLOSED' ? 'badge-warning' : ''}`}>
            {t(`cardBill.status.${bill.status ?? 'OPEN'}`)}
            {bill.partial ? ` · ${t('cardBill.partial')}` : ''}
          </span>
        </span>
        <MoneyText className="text-2xl font-semibold" value={bill.total} />
        <span className="text-xs text-(--color-text-muted)">
          {bill.closingDate && t('cardBill.closing', { date: formatDate(bill.closingDate) })}
          {bill.closingDate && bill.dueDate && ' · '}
          {bill.dueDate && t('cardBill.due', { date: formatDate(bill.dueDate) })}
        </span>
      </button>
      <dl className="grid grid-cols-3 gap-1 text-xs">
        <div>
          <dt className="text-(--color-text-muted)">{t('cardBill.single')}</dt>
          <dd>
            <MoneyText value={bill.single} />
          </dd>
        </div>
        <div>
          <dt className="text-(--color-text-muted)">{t('cardBill.installments')}</dt>
          <dd>
            <MoneyText value={bill.installments} />
          </dd>
        </div>
        <div>
          <dt className="text-(--color-text-muted)">{t('cardBill.subscriptions')}</dt>
          <dd>
            <MoneyText value={bill.subscriptions} />
          </dd>
        </div>
      </dl>
      <p className="text-xs text-(--color-text-muted)">
        {t('cardBill.committed')}: {formatMoney(bill.committedFuture)}
        {bill.fileTotal != null && ` · ${t('cardBill.fileTotal', { amount: formatMoney(bill.fileTotal) })}`}
      </p>
      {(bill.unconfirmedCount ?? 0) > 0 && (
        <p className="text-xs text-amber-700">{t('cardBill.unconfirmed', { count: bill.unconfirmedCount })}</p>
      )}
      <div className="flex justify-end gap-2">
        {linkOf(bill, 'import') && (
          <button type="button" className="button" onClick={() => navigate('/import')}>
            <FileUp size={14} aria-hidden /> {t('cardBill.import')}
          </button>
        )}
        {pay.available && (
          <button
            type="button"
            className="button"
            onClick={() => pay.mutate({ method: 'POST', version: `"${bill.version ?? 0}"` })}
          >
            <Wallet size={14} aria-hidden /> {t('cardBill.markPaid')}
          </button>
        )}
      </div>
    </article>
  );
}

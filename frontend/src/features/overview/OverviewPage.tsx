import { AlertTriangle, ArrowRight } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { useMonthlySummary } from '../../api/generated/endpoints/monthly-summary-controller/monthly-summary-controller';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { SummaryCard } from '../../shared/components/SummaryCard';
import { formatMoney, formatPercent, formatPeriod } from '../../shared/format';
import { useCategoryMap, useMemberMap } from '../../shared/hooks/useLookups';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { useSettingsMap } from '../settings/GeneralSettings';
import { CategoryChart } from './CategoryChart';
import { MemberTotalsTable } from './MemberTotalsTable';

const TONES = { NONE: 'success', WARNING: 'warning', DANGER: 'danger' } as const;

/**
 * Tab 1, overview of the billing period: indicators with alert (RF-10, RF-16), expense by category
 * (RF-12), totals by member (RF-13) and card (RF-14), what is committed ahead (RF-17), who owes whom
 * (RF-27) and the pending block (RF-79a); an optional member filter narrows everything (RF-15).
 *
 * @returns the page
 */
export function OverviewPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const period = useBillingPeriod();
  const [member, setMember] = useState<number | null>(null);
  const { data: summary } = useMonthlySummary(period, member == null ? undefined : { member });
  const settings = useSettingsMap();
  const categories = useCategoryMap();
  const members = useMemberMap();
  const alert = (summary?.alert ?? 'NONE') as keyof typeof TONES;
  const threshold = alert === 'DANGER' ? (settings['alert-danger-percent'] ?? '100') : (settings['alert-warning-percent'] ?? '80');
  const memberName = (id: number | undefined) => members.get(id ?? 0)?.code ?? String(id);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-xl font-semibold">{t('nav.overview')}</h1>
        <Field label={t('overview.memberFilter')} htmlFor="overview-member">
          <MemberPicker id="overview-member" allowEmpty emptyLabel={t('overview.everyone')} includeInactive value={member} onChange={setMember} />
        </Field>
      </div>

      {alert !== 'NONE' && (
        <p role="alert" className={`panel flex items-center gap-2 ${alert === 'DANGER' ? 'text-red-700' : 'text-amber-700'}`}>
          <AlertTriangle size={18} aria-hidden />
          {alert === 'DANGER' ? t('overview.alertDanger', { percent: threshold }) : t('overview.alertWarning', { percent: threshold })}
        </p>
      )}

      <div className="grid gap-3 sm:grid-cols-2 wide:grid-cols-4">
        <SummaryCard
          title={t('overview.income')}
          value={<MoneyText value={summary?.income} />}
          hint={
            member == null
              ? t('overview.incomeHint', { main: formatMoney(summary?.mainIncome), reimbursements: formatMoney(summary?.reimbursements) })
              : undefined
          }
        />
        <SummaryCard
          title={t('overview.expense')}
          value={<MoneyText value={summary?.expense} />}
          hint={t('overview.expenseHint', {
            fixed: formatMoney(summary?.fixedTotal),
            variable: formatMoney(summary?.variableTotal),
            card: formatMoney(summary?.cardTotal),
          })}
        />
        <SummaryCard
          title={t('overview.balance')}
          value={<MoneyText value={summary?.balance} kind="balance" />}
        />
        <SummaryCard
          title={t('overview.percentSpent')}
          tone={TONES[alert]}
          value={summary?.percentSpent == null ? t('overview.noIncome') : formatPercent(summary.percentSpent)}
        />
      </div>

      {(summary?.pendingCount ?? 0) > 0 && (
        <div className="panel flex flex-wrap items-center justify-between gap-2 border-amber-500">
          <span>{t('overview.pending', { count: summary?.pendingCount, amount: formatMoney(summary?.pendingAmount) })}</span>
          <button type="button" className="button" onClick={() => navigate(`/review-queue?billingPeriod=${period}`)}>
            {t('overview.reviewNow')} <ArrowRight size={14} aria-hidden />
          </button>
        </div>
      )}
      {(summary?.unconfirmedCount ?? 0) > 0 && (
        <p className="text-sm text-(--color-text-muted)">{t('overview.unconfirmed', { count: summary?.unconfirmedCount })}</p>
      )}

      <section className="grid gap-4 wide:grid-cols-2" aria-labelledby="by-category-title">
        <h2 id="by-category-title" className="text-lg font-semibold wide:col-span-2">
          {t('overview.byCategory')}
        </h2>
        <CategoryChart rows={summary?.categories ?? []} />
        <div className="overflow-x-auto">
          <table className="table">
            <thead>
              <tr>
                <th scope="col">{t('overview.category')}</th>
                <th scope="col" className="text-right">
                  {t('recurrence.amount')}
                </th>
                <th scope="col" className="text-right">
                  {t('overview.share')}
                </th>
                <th scope="col" className="text-right">
                  {t('overview.variation')}
                </th>
              </tr>
            </thead>
            <tbody>
              {(summary?.categories ?? []).map((row) => {
                const category = row.categoryId != null ? categories.get(row.categoryId) : undefined;
                return (
                  <tr key={row.categoryId ?? 'none'}>
                    <th scope="row" className="font-normal">
                      <span className="inline-flex items-center gap-2">
                        <CategoryIcon icon={category?.icon} color={category?.color ?? '#9aa4af'} />
                        {category?.name ?? t('overview.unclassified')}
                      </span>
                    </th>
                    <td className="text-right">
                      <MoneyText value={row.amount} />
                    </td>
                    <td className="text-right">{formatPercent(row.percentOfTotal)}</td>
                    <td className={`text-right ${(row.variation ?? 0) > 0 ? 'text-red-600' : 'text-emerald-700'}`}>
                      {row.variation == null ? '—' : `${row.variation > 0 ? '+' : ''}${formatPercent(row.variation)}`}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      <section className="flex flex-col gap-3" aria-labelledby="by-member-title">
        <h2 id="by-member-title" className="text-lg font-semibold">
          {t('overview.byMember')}
        </h2>
        <MemberTotalsTable rows={summary?.members ?? []} caption={t('overview.byMember')} />
      </section>

      <div className="grid gap-4 wide:grid-cols-3">
        <section className="flex flex-col gap-2 wide:col-span-1" aria-labelledby="by-card-title">
          <h2 id="by-card-title" className="text-lg font-semibold">
            {t('overview.byCard')}
          </h2>
          <ul className="flex flex-col gap-2">
            {(summary?.cards ?? []).map((card) => (
              <li key={card.cardId}>
                <button
                  type="button"
                  className="panel flex w-full items-center justify-between gap-2 text-left"
                  onClick={() => navigate(`/entries?billingPeriod=${period}&card=${card.cardId}`)}
                  aria-label={t('cardBill.open', { card: card.nickname })}
                >
                  <span>
                    <span className="block font-semibold">{card.nickname}</span>
                    <span className="block text-xs text-(--color-text-muted)">
                      {t('cardBill.committed')}: {formatMoney(card.committedFuture)}
                    </span>
                  </span>
                  <MoneyText value={card.total} />
                </button>
              </li>
            ))}
          </ul>
        </section>
        <section className="flex flex-col gap-2" aria-labelledby="committed-title">
          <h2 id="committed-title" className="text-lg font-semibold">
            {t('overview.committedNext')}
          </h2>
          <ul className="panel flex flex-col gap-1">
            {(summary?.committedNext ?? []).map((row) => (
              <li key={row.billingPeriod} className="flex justify-between">
                <span>{formatPeriod(row.billingPeriod ?? '')}</span>
                <MoneyText value={row.amount} />
              </li>
            ))}
          </ul>
        </section>
        <section className="flex flex-col gap-2" aria-labelledby="settlements-title">
          <h2 id="settlements-title" className="text-lg font-semibold">
            {t('overview.settlements')}
          </h2>
          <ul className="panel flex flex-col gap-1">
            {(summary?.settlements ?? []).length === 0 && <li className="text-(--color-text-muted)">{t('overview.noSettlements')}</li>}
            {(summary?.settlements ?? []).map((row) => (
              <li key={`${row.fromMemberId}-${row.toMemberId}`}>
                {t('overview.owes', { from: memberName(row.fromMemberId), to: memberName(row.toMemberId), amount: formatMoney(row.amount) })}
              </li>
            ))}
          </ul>
        </section>
      </div>
    </div>
  );
}

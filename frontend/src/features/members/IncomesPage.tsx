import { useTranslation } from 'react-i18next';
import { useMonthlySummary } from '../../api/generated/endpoints/monthly-summary-controller/monthly-summary-controller';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { MemberTotalsTable } from '../overview/MemberTotalsTable';
import { OccurrencesSection } from '../recurrence/OccurrencesSection';
import { CarryOverButton } from './CarryOverButton';
import { Link } from 'react-router';

/**
 * Tab 2, incomes and members (SPEC section 5): incomes of the period (RF-21 to RF-24), total expense by
 * member (RF-25) with the carry-over of what third parties still owe (RF-28) and a link to the members registry
 * (RF-20).
 *
 * @returns the page
 */
export function IncomesPage() {
  const { t } = useTranslation();
  const period = useBillingPeriod();
  const { data: summary } = useMonthlySummary(period);
  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-xl font-semibold">{t('nav.incomes')}</h1>
      <OccurrencesSection kind="INCOME" period={period} />
      <section className="flex flex-col gap-3" aria-labelledby="member-totals-title">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 id="member-totals-title" className="text-lg font-semibold">
            {t('overview.totalsByMember')}
          </h2>
          <CarryOverButton period={period} rows={summary?.members ?? []} />
        </div>
        <MemberTotalsTable rows={summary?.members ?? []} caption={t('overview.totalsByMember')} />
      </section>
      <Link className="button self-start" to="/registry">
        {t('registry.manageMembers')}
      </Link>
    </div>
  );
}

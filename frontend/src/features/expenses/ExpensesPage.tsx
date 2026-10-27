import { useTranslation } from 'react-i18next';
import { useBillingPeriod } from '../../shared/state/billingPeriod';
import { OccurrencesSection } from '../recurrence/OccurrencesSection';
import { CardBillCards } from './CardBillCards';
import { VariableExpensesSection } from './VariableExpensesSection';

/**
 * Tab 3, expenses of the billing period: card totals, fixed expenses and variable expenses (SPEC section 6).
 *
 * @returns the page
 */
export function ExpensesPage() {
  const { t } = useTranslation();
  const period = useBillingPeriod();
  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-xl font-semibold">{t('nav.expenses')}</h1>
      <CardBillCards period={period} />
      <OccurrencesSection kind="FIXED_EXPENSE" period={period} />
      <VariableExpensesSection period={period} />
    </div>
  );
}

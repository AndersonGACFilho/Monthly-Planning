import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import type { CategoryRow } from '../../api/generated/model';
import { CategoryIcon } from '../../shared/components/CategoryIcon';
import { MoneyText } from '../../shared/components/MoneyText';
import { formatPercent } from '../../shared/format';
import { useCategoryMap } from '../../shared/hooks/useLookups';
import { CategoryChart, UNCLASSIFIED_COLOR } from './CategoryChart';
import { categoryEntriesPath } from './categoryEntriesPath';

/** Props of {@link CategoryBreakdown}. */
export interface CategoryBreakdownProps {
  /** Category rows of the overview, largest first. */
  rows: CategoryRow[];
  /** Billing period shown. */
  period: string;
  /** Member filter of the overview, or `null` for everyone. */
  member: number | null;
}

/**
 * Expense by category as a doughnut and a table (RF-12, UI-07); a slice or a category name opens the
 * Entries tab filtered by that category.
 *
 * @param props - {@link CategoryBreakdownProps}
 * @returns the section
 */
export function CategoryBreakdown({ rows, period, member }: CategoryBreakdownProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const categories = useCategoryMap();
  // Entries without a category are listed under the system "not classified" category
  const unclassifiedId = [...categories.values()].find((c) => c.system)?.id;
  const targetOf = (row: CategoryRow) => row.categoryId ?? unclassifiedId;
  const open = (row: CategoryRow) => {
    const target = targetOf(row);
    if (target != null) navigate(categoryEntriesPath(period, target, member));
  };

  return (
    <section className="grid gap-4 wide:grid-cols-2" aria-labelledby="by-category-title">
      <h2 id="by-category-title" className="text-lg font-semibold wide:col-span-2">
        {t('overview.byCategory')}
      </h2>
      <CategoryChart rows={rows} onSelect={open} />
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
            {rows.map((row) => {
              const category = row.categoryId != null ? categories.get(row.categoryId) : undefined;
              const name = category?.name ?? t('overview.unclassified');
              return (
                <tr key={row.categoryId ?? 'none'}>
                  <th scope="row" className="font-normal">
                    <button
                      type="button"
                      className="inline-flex items-center gap-2 text-left hover:underline disabled:no-underline"
                      disabled={targetOf(row) == null}
                      title={t('overview.openCategory', { category: name })}
                      onClick={() => open(row)}
                    >
                      <CategoryIcon icon={category?.icon} color={category?.color ?? UNCLASSIFIED_COLOR} />
                      {name}
                    </button>
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
  );
}

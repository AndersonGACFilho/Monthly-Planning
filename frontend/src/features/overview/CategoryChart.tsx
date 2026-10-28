import { useTranslation } from 'react-i18next';
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { CategoryRow } from '../../api/generated/model';
import { formatMoney } from '../../shared/format';
import { useCategoryMap } from '../../shared/hooks/useLookups';

/** Colour of expenses without a category. */
export const UNCLASSIFIED_COLOR = '#9aa4af';

/** Props of {@link CategoryChart}. */
export interface CategoryChartProps {
  /** Category rows of the overview. */
  rows: CategoryRow[];
  /** Called with the row of the slice the user clicks. */
  onSelect: (row: CategoryRow) => void;
}

/**
 * Doughnut chart of the expense by category (RF-12, UI-07). Only positive totals are drawn.
 *
 * @param props - {@link CategoryChartProps}
 * @returns the chart
 */
export function CategoryChart({ rows, onSelect }: CategoryChartProps) {
  const { t } = useTranslation();
  const categories = useCategoryMap();
  const data = rows
    .filter((row) => (row.amount ?? 0) > 0)
    .map((row) => {
      const category = row.categoryId != null ? categories.get(row.categoryId) : undefined;
      return {
        row,
        name: category?.name ?? t('overview.unclassified'),
        value: row.amount ?? 0,
        color: category?.color ?? UNCLASSIFIED_COLOR,
      };
    });
  return (
    <figure className="h-64 w-full" aria-label={t('overview.chart')}>
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="name"
            innerRadius="55%"
            outerRadius="85%"
            isAnimationActive={false}
            className="cursor-pointer"
            onClick={(_, index) => {
              const slice = data[index];
              if (slice) onSelect(slice.row);
            }}
          >
            {data.map((slice) => (
              <Cell key={slice.name} fill={slice.color} />
            ))}
          </Pie>
          <Tooltip formatter={(value) => formatMoney(Number(value))} />
        </PieChart>
      </ResponsiveContainer>
    </figure>
  );
}

import { ArrowDown, ArrowUp } from 'lucide-react';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

/** One column of a {@link FilterableList}. */
export interface Column<T> {
  /** Stable key. */
  key: string;
  /** Header text. */
  header: string;
  /** Cell content. */
  render: (row: T) => ReactNode;
  /** Value used to sort; columns without it cannot be sorted. */
  sortValue?: (row: T) => string | number;
  /** Align numbers to the right. */
  numeric?: boolean;
  /** Hide below 1200 px (SPEC section 14: essential columns only). */
  wideOnly?: boolean;
}

/** Props of {@link FilterableList}. */
export interface FilterableListProps<T> {
  /** Rows. */
  rows: T[];
  /** Columns. */
  columns: Column<T>[];
  /** Row key. */
  rowKey: (row: T) => string | number;
  /** Text used by the search box; omit to hide it. */
  searchText?: (row: T) => string;
  /** Extra filters shown next to the search box. */
  filters?: ReactNode;
  /** Initial sort column key. */
  initialSort?: string;
  /** Called with the rows left after search and filters, such as for the quick total (RF-42). */
  onVisibleRowsChange?: (rows: T[]) => void;
  /** Row actions shown in the last column. */
  actions?: (row: T) => ReactNode;
  /** Message when there are no rows. */
  emptyMessage?: string;
}

/**
 * Table with text search, extra filters and sorting by any column; shared by entries, variable expenses
 * and incomes (SPEC section 13, RF-35, RF-41).
 *
 * @param props - {@link FilterableListProps}
 * @returns the table
 */
export function FilterableList<T>({
  rows,
  columns,
  rowKey,
  searchText,
  filters,
  initialSort,
  onVisibleRowsChange,
  actions,
  emptyMessage,
}: FilterableListProps<T>) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState<{ key: string; asc: boolean } | null>(
    initialSort ? { key: initialSort, asc: true } : null,
  );

  const visible = useMemo(() => {
    const needle = query.trim().toLowerCase();
    let result = needle && searchText ? rows.filter((row) => searchText(row).toLowerCase().includes(needle)) : rows;
    const column = columns.find((c) => c.key === sort?.key);
    if (column?.sortValue && sort) {
      const value = column.sortValue;
      result = [...result].sort((a, b) => {
        const x = value(a);
        const y = value(b);
        const order = typeof x === 'number' && typeof y === 'number' ? x - y : String(x).localeCompare(String(y));
        return sort.asc ? order : -order;
      });
    }
    return result;
  }, [rows, columns, query, sort, searchText]);

  useEffect(() => {
    onVisibleRowsChange?.(visible);
  }, [visible, onVisibleRowsChange]);

  return (
    <div className="flex flex-col gap-2">
      {(searchText || filters) && (
        <div className="flex flex-wrap items-end gap-2">
          {searchText && (
            <input
              type="search"
              className="input w-64"
              placeholder={t('common.search')}
              aria-label={t('common.search')}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          )}
          {filters}
        </div>
      )}
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="border-b border-(--color-border) text-left text-(--color-text-muted)">
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={`px-2 py-1 font-medium ${column.numeric ? 'text-right' : ''} ${column.wideOnly ? 'hidden wide:table-cell' : ''}`}
                aria-sort={sort?.key === column.key ? (sort.asc ? 'ascending' : 'descending') : undefined}
              >
                {column.sortValue ? (
                  <button
                    type="button"
                    className="inline-flex items-center gap-1"
                    onClick={() =>
                      setSort((current) =>
                        current?.key === column.key ? { key: column.key, asc: !current.asc } : { key: column.key, asc: true },
                      )
                    }
                  >
                    {column.header}
                    {sort?.key === column.key &&
                      (sort.asc ? <ArrowUp size={12} aria-hidden /> : <ArrowDown size={12} aria-hidden />)}
                  </button>
                ) : (
                  column.header
                )}
              </th>
            ))}
            {actions && <th scope="col" className="px-2 py-1" aria-label={t('common.actions')} />}
          </tr>
        </thead>
        <tbody>
          {visible.map((row) => (
            <tr key={rowKey(row)} className="border-b border-(--color-border) hover:bg-(--color-surface-muted)">
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`px-2 py-1 ${column.numeric ? 'text-right tabular-nums' : ''} ${column.wideOnly ? 'hidden wide:table-cell' : ''}`}
                >
                  {column.render(row)}
                </td>
              ))}
              {actions && <td className="px-2 py-1 text-right whitespace-nowrap">{actions(row)}</td>}
            </tr>
          ))}
          {visible.length === 0 && (
            <tr>
              <td colSpan={columns.length + (actions ? 1 : 0)} className="px-2 py-4 text-center text-(--color-text-muted)">
                {emptyMessage ?? t('common.empty')}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

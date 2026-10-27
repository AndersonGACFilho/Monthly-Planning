import { useQueryClient } from '@tanstack/react-query';
import { useVirtualizer } from '@tanstack/react-virtual';
import { Plus } from 'lucide-react';
import { useEffect, useMemo, useRef, useState, type MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { useBillEntries } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import type { BillEntryModel, CardModel } from '../../api/generated/model';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { QuickTotalBar } from '../../shared/components/QuickTotalBar';
import { formatDate } from '../../shared/format';
import { embedded } from '../../shared/hal';
import { useBillingPeriod, useBillingPeriodStore } from '../../shared/state/billingPeriod';
import { CategoryCell, DescriptionCell, MemberCell } from './EntryCells';
import { EntryDetails } from './EntryDetails';
import { ManualEntryForm } from './ManualEntryForm';
import { selectRows } from './selection';

const ROW_HEIGHT = 44;
/** Lists longer than this are virtualized (UI-09); shorter ones render every row. */
const VIRTUALIZE_FROM = 100;

/**
 * Tab 4, card entries of the billing period: filters, sortable virtualized table, inline editing, quick
 * total and details panel (RF-40 to RF-45, UI-03, UI-04, UI-05, UI-09). Filters live in the URL, so the
 * card totals can open the tab already filtered (RF-38).
 *
 * @returns the page
 */
export function EntriesPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const period = useBillingPeriod();
  const setPeriod = useBillingPeriodStore((state) => state.setPeriod);
  const [params, setParams] = useSearchParams();
  const linkedPeriod = params.get('billingPeriod');

  // A link may carry a billing period (RF-38): it becomes the global period shown at the top (UI-02)
  useEffect(() => {
    if (!linkedPeriod) return;
    setPeriod(linkedPeriod);
    const next = new URLSearchParams(params);
    next.delete('billingPeriod');
    setParams(next, { replace: true });
  }, [linkedPeriod, params, setParams, setPeriod]);

  const filter = {
    card: params.get('card') ? Number(params.get('card')) : undefined,
    member: params.get('member') ? Number(params.get('member')) : undefined,
    category: params.get('category') ? Number(params.get('category')) : undefined,
    type: params.get('type') ?? undefined,
    q: params.get('q') ?? undefined,
  };
  const [sort, setSort] = useState('entryDate,asc');
  const { data } = useBillEntries({ billingPeriod: linkedPeriod ?? period, ...filter, size: 2000, sort: [sort] });
  const { data: cardPage } = useCards({ size: 200 });
  const entries = embedded<BillEntryModel>(data, 'billEntries');
  const ids = useMemo(() => entries.map((e) => e.id!), [entries]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [anchor, setAnchor] = useState<number | null>(null);
  const [focused, setFocused] = useState<number | null>(null);
  const [creating, setCreating] = useState(params.get('new') === '1');
  // Ctrl+N opens this tab with ?new=1 (UI-01): show the manual entry form and drop the flag
  useEffect(() => {
    if (params.get('new') !== '1') return;
    setCreating(true);
    const next = new URLSearchParams(params);
    next.delete('new');
    setParams(next, { replace: true });
  }, [params, setParams]);
  const scroller = useRef<HTMLDivElement>(null);
  // eslint-disable-next-line react-hooks/incompatible-library -- the virtualizer is read on every render on purpose (UI-09)
  const virtualizer = useVirtualizer({
    count: entries.length,
    getScrollElement: () => scroller.current,
    estimateSize: () => ROW_HEIGHT,
    initialRect: { width: 1200, height: 800 },
    overscan: 12,
  });
  const refresh = () => void queryClient.invalidateQueries();

  const setFilter = (key: string, value: string | number | undefined | null) => {
    const next = new URLSearchParams(params);
    if (value != null && value !== '') next.set(key, String(value));
    else next.delete(key);
    setParams(next);
  };

  const onRowClick = (event: MouseEvent, id: number) => {
    if ((event.target as HTMLElement).closest('button, select, input, a')) return;
    setSelected(selectRows(selected, ids, id, anchor, { ctrl: event.ctrlKey || event.metaKey, shift: event.shiftKey }));
    setAnchor(id);
    setFocused(id);
  };

  const sortBy = (property: string) => setSort((current) => (current === `${property},asc` ? `${property},desc` : `${property},asc`));
  const focusedEntry = entries.find((e) => e.id === focused);
  const virtualize = entries.length > VIRTUALIZE_FROM;
  const items = virtualize
    ? virtualizer.getVirtualItems()
    : entries.map((_, index) => ({ index, start: index * ROW_HEIGHT, end: (index + 1) * ROW_HEIGHT }));
  const paddingTop = virtualize ? (items[0]?.start ?? 0) : 0;
  const paddingBottom = virtualize ? virtualizer.getTotalSize() - (items.at(-1)?.end ?? 0) : 0;

  const header = (property: string | null, label: string, className = '') => (
    <th scope="col" className={`px-2 py-1 font-medium ${className}`} aria-sort={property && sort.startsWith(`${property},`) ? (sort.endsWith('asc') ? 'ascending' : 'descending') : undefined}>
      {property ? (
        <button type="button" onClick={() => sortBy(property)}>
          {label}
        </button>
      ) : (
        label
      )}
    </th>
  );

  return (
    <div className="flex h-full flex-col gap-3">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold">{t('nav.entries')}</h1>
        <button type="button" className="button" onClick={() => setCreating(true)}>
          <Plus size={16} aria-hidden /> {t('entries.newEntry')}
        </button>
      </div>
      {creating && <ManualEntryForm onDone={() => setCreating(false)} />}
      <div className="flex flex-wrap items-end gap-3" role="search">
        <Field label={t('import.card')} htmlFor="entries-card">
          <select id="entries-card" className="input" value={filter.card ?? ''} onChange={(e) => setFilter('card', e.target.value)}>
            <option value="">{t('common.all')}</option>
            {embedded<CardModel>(cardPage, 'cards').filter((c) => c.primaryCardId == null).map((c) => (
              <option key={c.id} value={c.id}>
                {c.nickname}
              </option>
            ))}
          </select>
        </Field>
        <Field label={t('review.member')} htmlFor="entries-member">
          <MemberPicker id="entries-member" allowEmpty includeInactive value={filter.member ?? null} onChange={(v) => setFilter('member', v)} />
        </Field>
        <Field label={t('review.category')} htmlFor="entries-category">
          <CategoryPicker id="entries-category" allowEmpty value={filter.category ?? null} onChange={(v) => setFilter('category', v)} />
        </Field>
        <Field label={t('entries.type')} htmlFor="entries-type">
          <select id="entries-type" className="input" value={filter.type ?? ''} onChange={(e) => setFilter('type', e.target.value)}>
            <option value="">{t('common.all')}</option>
            <option value="-">{t('entries.types.single')}</option>
            <option value="n/N">{t('entries.types.installment')}</option>
            <option value="*">{t('entries.types.subscription')}</option>
          </select>
        </Field>
        <Field label={t('common.search')} htmlFor="entries-search">
          <input id="entries-search" type="search" className="input w-64" defaultValue={filter.q} onChange={(e) => setFilter('q', e.target.value)} />
        </Field>
      </div>

      <div className="flex min-h-0 flex-1 gap-3">
        <div ref={scroller} className="min-h-0 flex-1 overflow-auto">
          <table className="w-full border-collapse text-sm">
            <thead className="sticky top-0 z-10 bg-(--color-surface)">
              <tr className="border-b border-(--color-border) text-left text-(--color-text-muted)">
                <th scope="col" className="px-2 py-1">
                  <input
                    type="checkbox"
                    aria-label={t('review.selectAll')}
                    checked={entries.length > 0 && selected.size === entries.length}
                    onChange={(e) => setSelected(e.target.checked ? new Set(ids) : new Set())}
                  />
                </th>
                {header('entryDate', t('entry.date'))}
                {header('displayDescription', t('entry.description'))}
                {header('installment', t('entry.installment'))}
                {header(null, t('entry.card'), 'hidden wide:table-cell')}
                {header(null, t('review.member'))}
                {header(null, t('review.category'))}
                {header('amount', t('entry.amount'), 'text-right')}
                {header('status', t('common.status'), 'hidden wide:table-cell')}
              </tr>
            </thead>
            <tbody>
              {paddingTop > 0 && (
                <tr aria-hidden>
                  <td style={{ height: paddingTop }} />
                </tr>
              )}
              {items.map((item) => {
                const entry = entries[item.index]!;
                const isSelected = selected.has(entry.id!);
                return (
                  <tr
                    key={entry.id}
                    data-index={item.index}
                    aria-selected={isSelected}
                    onClick={(e) => onRowClick(e, entry.id!)}
                    className={`border-b border-(--color-border) ${isSelected ? 'bg-(--color-surface-muted)' : ''} ${
                      entry.status === 'PROJECTED' ? 'text-(--color-text-muted) italic' : ''
                    } ${entry.reviewStatus === 'PENDING_REVIEW' ? 'border-l-4 border-l-amber-500' : ''}`}
                    style={{ height: ROW_HEIGHT }}
                  >
                    <td className="px-2">
                      <input
                        type="checkbox"
                        aria-label={t('review.select')}
                        checked={isSelected}
                        onChange={() => setSelected(selectRows(selected, ids, entry.id!, anchor, { ctrl: true, shift: false }))}
                      />
                    </td>
                    <td className="px-2 whitespace-nowrap">{formatDate(entry.entryDate)}</td>
                    <td className="px-2">
                      <DescriptionCell entry={entry} onChanged={refresh} />
                    </td>
                    <td className="px-2">{entry.installment}</td>
                    <td className="hidden px-2 wide:table-cell">{entry.cardLastDigits}</td>
                    <td className="px-2">
                      <MemberCell entry={entry} onChanged={refresh} />
                    </td>
                    <td className="px-2">
                      <CategoryCell entry={entry} onChanged={refresh} />
                    </td>
                    <td className="px-2 text-right">
                      <MoneyText value={entry.amount} />
                    </td>
                    <td className="hidden px-2 wide:table-cell">{t(`entries.status.${entry.status}`)}</td>
                  </tr>
                );
              })}
              {paddingBottom > 0 && (
                <tr aria-hidden>
                  <td style={{ height: paddingBottom }} />
                </tr>
              )}
              {entries.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-2 py-6 text-center text-(--color-text-muted)">
                    {t('common.empty')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {focusedEntry && <EntryDetails key={focusedEntry.id} entry={focusedEntry} />}
      </div>
      <QuickTotalBar
        filtered={entries.map((e) => e.amount)}
        selected={entries.filter((e) => selected.has(e.id!)).map((e) => e.amount)}
      />
    </div>
  );
}

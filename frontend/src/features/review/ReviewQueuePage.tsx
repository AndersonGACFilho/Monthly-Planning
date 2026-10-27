import { useQueryClient } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import {
  useReviewQueue,
  validateSelected,
} from '../../api/generated/endpoints/review-queue-controller/review-queue-controller';
import type { CardModel, ReviewItemModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { embedded, linkOf } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';
import { fromCents, toCents } from '../../shared/format';
import { ReviewRow } from './ReviewRow';

/**
 * Review queue: imported purchases waiting for member, split, category and note, with filters, batch
 * actions and keyboard flow (RF-74 to RF-79).
 *
 * @returns the page
 */
export function ReviewQueuePage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const saved = useSaved();
  const [params, setParams] = useSearchParams();
  const card = params.get('card') ? Number(params.get('card')) : undefined;
  const billingPeriod = params.get('billingPeriod') ?? undefined;
  const batch = params.get('batch') ? Number(params.get('batch')) : undefined;
  const { data } = useReviewQueue({ card, billingPeriod, batch });
  const { data: cardPage } = useCards({ size: 200 });
  const items = embedded<ReviewItemModel>(data, 'items');
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [batchMember, setBatchMember] = useState<number | null>(null);
  const [batchCategory, setBatchCategory] = useState<number | null>(null);
  const rows = useRef<(HTMLTableRowElement | null)[]>([]);
  const total = fromCents(items.reduce((sum, i) => sum + toCents(i.amount), 0));

  const setFilter = (key: string, value: string | undefined) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next);
  };

  const focusRow = (index: number) => {
    const row = rows.current[index];
    row?.querySelector<HTMLElement>('select, input:not([type=checkbox])')?.focus();
  };

  const applyToSelected = async (validate: boolean) => {
    try {
      await validateSelected({
        purchaseIds: [...selected],
        changes: {
          splits: batchMember == null ? undefined : [{ memberId: batchMember, percentage: 100 }],
          categoryId: batchCategory ?? undefined,
          clearCategory: false,
          subscription: false,
        },
        validate,
      });
      setSelected(new Set());
      await saved();
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold">{t('nav.reviewQueue')}</h1>
      <div className="flex flex-wrap items-end gap-3">
        <Field label={t('import.card')} htmlFor="review-card">
          <select id="review-card" className="input" value={card ?? ''} onChange={(e) => setFilter('card', e.target.value || undefined)}>
            <option value="">{t('common.all')}</option>
            {embedded<CardModel>(cardPage, 'cards').filter((c) => c.primaryCardId == null).map((c) => (
              <option key={c.id} value={c.id}>
                {c.nickname}
              </option>
            ))}
          </select>
        </Field>
        <Field label={t('period.label')} htmlFor="review-period">
          <input id="review-period" type="month" className="input" value={billingPeriod ?? ''} onChange={(e) => setFilter('billingPeriod', e.target.value || undefined)} />
        </Field>
        {batch && (
          <button type="button" className="button" onClick={() => setFilter('batch', undefined)}>
            {t('review.batchFilter', { id: batch })} <X size={14} aria-hidden />
          </button>
        )}
        <p className="ml-auto text-sm">
          {t('review.pendingCount', { count: items.length })} · <MoneyText value={total} />
        </p>
      </div>

      {selected.size > 0 && (
        <div className="panel flex flex-wrap items-end gap-3" role="group" aria-label={t('review.batchActions')}>
          <span className="text-sm">{t('review.selectedCount', { count: selected.size })}</span>
          <Field label={t('review.member')} htmlFor="batch-member">
            <MemberPicker id="batch-member" allowEmpty value={batchMember} onChange={setBatchMember} />
          </Field>
          <Field label={t('review.category')} htmlFor="batch-category">
            <CategoryPicker id="batch-category" allowEmpty value={batchCategory} onChange={setBatchCategory} />
          </Field>
          <button type="button" className="button" onClick={() => void applyToSelected(false)}>
            {t('review.applyToSelected')}
          </button>
          {linkOf(data, 'validate-selected') && (
            <button type="button" className="button button-primary" onClick={() => void applyToSelected(true)}>
              {t('review.validateSelected')}
            </button>
          )}
        </div>
      )}

      {items.length === 0 ? (
        <p className="panel text-(--color-text-muted)">{t('review.empty')}</p>
      ) : (
        <table className="w-full border-collapse text-sm">
          <caption className="sr-only">{t('review.keyboardHint')}</caption>
          <thead>
            <tr className="border-b border-(--color-border) text-left text-(--color-text-muted)">
              <th className="px-2 py-1">
                <input
                  type="checkbox"
                  aria-label={t('review.selectAll')}
                  checked={selected.size === items.length}
                  onChange={(e) => setSelected(e.target.checked ? new Set(items.map((i) => i.purchaseId!)) : new Set())}
                />
              </th>
              <th className="px-2 py-1">{t('entry.date')}</th>
              <th className="px-2 py-1">{t('entry.description')}</th>
              <th className="px-2 py-1">{t('entry.installment')}</th>
              <th className="px-2 py-1 text-right">{t('entry.amount')}</th>
              <th className="px-2 py-1">{t('review.member')}</th>
              <th className="px-2 py-1">{t('review.category')}</th>
              <th className="hidden px-2 py-1 wide:table-cell">{t('review.note')}</th>
              <th className="px-2 py-1" aria-label={t('common.actions')} />
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <ReviewRow
                key={`${item.purchaseId}-${item.version}`}
                ref={(el) => {
                  rows.current[index] = el;
                }}
                item={item}
                selected={selected.has(item.purchaseId!)}
                onSelect={(isSelected) => {
                  const next = new Set(selected);
                  if (isSelected) next.add(item.purchaseId!);
                  else next.delete(item.purchaseId!);
                  setSelected(next);
                }}
                onNext={() => focusRow(index + 1)}
                onValidated={() => void queryClient.invalidateQueries()}
              />
            ))}
          </tbody>
        </table>
      )}
      <p className="text-xs text-(--color-text-muted)">{t('review.keyboardHint')}</p>
    </div>
  );
}

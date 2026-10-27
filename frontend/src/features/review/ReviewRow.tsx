import { forwardRef, useState, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { validateItem } from '../../api/generated/endpoints/review-queue-controller/review-queue-controller';
import type { ReviewItemModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { MoneyText } from '../../shared/components/MoneyText';
import { SplitEditor, type SplitShare } from '../../shared/components/SplitEditor';
import { formatDate } from '../../shared/format';

/** Props of {@link ReviewRow}. */
export interface ReviewRowProps {
  /** Pending item. */
  item: ReviewItemModel;
  /** Selected for batch actions. */
  selected: boolean;
  /** Toggles the selection. */
  onSelect: (selected: boolean) => void;
  /** Moves the focus to the next item (Enter after validating, Esc to skip, RF-78). */
  onNext: () => void;
  /** Called after the item was validated. */
  onValidated: () => void;
}

/**
 * One pending item: member split, category, note and subscription suggestion, validated with Enter
 * (RF-75, RF-76, RF-78, RN-23).
 */
export const ReviewRow = forwardRef<HTMLTableRowElement, ReviewRowProps>(function ReviewRow(
  { item, selected, onSelect, onNext, onValidated },
  ref,
) {
  const { t } = useTranslation();
  const [splits, setSplits] = useState<SplitShare[]>(
    (item.splits ?? []).map((s) => ({ memberId: s.memberId ?? null, percentage: Number(s.percentage ?? 100) })),
  );
  const [categoryId, setCategoryId] = useState<number | null>(item.categoryId ?? null);
  const [note, setNote] = useState(item.note ?? '');
  const [subscription, setSubscription] = useState(false);

  const validate = async () => {
    try {
      await validateItem(
        item.purchaseId!,
        {
          splits: splits.filter((s) => s.memberId != null).map((s) => ({ memberId: s.memberId!, percentage: s.percentage })),
          categoryId: categoryId ?? undefined,
          clearCategory: categoryId == null,
          note,
          subscription,
        },
        ifMatch(item.version),
      );
      onValidated();
      onNext();
    } catch (error) {
      reportError(error);
    }
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTableRowElement>) => {
    if (event.key === 'Enter' && !(event.target instanceof HTMLButtonElement)) {
      event.preventDefault();
      void validate();
    } else if (event.key === 'Escape') {
      event.preventDefault();
      onNext();
    }
  };

  return (
    <tr ref={ref} tabIndex={-1} onKeyDown={onKeyDown} className="border-b border-(--color-border) align-top focus-within:bg-(--color-surface-muted)">
      <td className="px-2 py-2">
        <input type="checkbox" aria-label={t('review.select')} checked={selected} onChange={(e) => onSelect(e.target.checked)} />
      </td>
      <td className="px-2 py-2 whitespace-nowrap">{formatDate(item.purchaseDate)}</td>
      <td className="px-2 py-2">
        <span className="font-medium">{item.originalDescription}</span>
        <span className="block text-xs text-(--color-text-muted)">
          {[item.bankCategory, item.cardLastDigits && `•••• ${item.cardLastDigits}`, item.billingPeriod].filter(Boolean).join(' · ')}
        </span>
        {item.unknownFinal && <span className="block text-xs text-amber-600">{t('import.unknownFinal')}</span>}
        {item.refundOfPurchaseId && <span className="block text-xs text-emerald-600">{t('review.refund')}</span>}
        {item.subscriptionSuggested && (
          <label className="mt-1 flex items-center gap-1 text-xs text-(--color-accent)">
            <input type="checkbox" checked={subscription} onChange={(e) => setSubscription(e.target.checked)} />
            {t('review.subscriptionSuggested')}
          </label>
        )}
      </td>
      <td className="px-2 py-2">{item.installment}</td>
      <td className="px-2 py-2 text-right">
        <MoneyText value={item.amount} />
      </td>
      <td className="px-2 py-2">
        <SplitEditor idPrefix={`review-${item.purchaseId}`} value={splits} onChange={setSplits} />
      </td>
      <td className="px-2 py-2">
        <CategoryPicker aria-label={t('review.category')} allowEmpty value={categoryId} onChange={setCategoryId} />
      </td>
      <td className="hidden px-2 py-2 wide:table-cell">
        <input className="input w-full" aria-label={t('review.note')} value={note} onChange={(e) => setNote(e.target.value)} />
      </td>
      <td className="px-2 py-2">
        <button type="button" className="button button-primary" onClick={() => void validate()}>
          {t('review.validate')}
        </button>
      </td>
    </tr>
  );
});

import { useQueryClient } from '@tanstack/react-query';
import { forwardRef, useState, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { validateItem } from '../../api/generated/endpoints/review-queue-controller/review-queue-controller';
import type { BillEntryModel, ReviewItemModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { MoneyText } from '../../shared/components/MoneyText';
import { SplitEditor, type SplitShare } from '../../shared/components/SplitEditor';
import { formatDate } from '../../shared/format';
import { DescriptionCell } from '../entries/EntryCells';
import { CategoryPickerWithAdd, MemberPickerWithAdd } from '../registry/QuickAddPickers';

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
  /** Card nickname shown with the final. */
  cardName?: string;
}

/**
 * One pending item: description with inline rename (alias), member split, category, note and subscription
 * suggestion, validated with Enter; members and categories can be registered on the spot (RF-46, RF-75, RF-76,
 * RF-78, RN-23).
 */
export const ReviewRow = forwardRef<HTMLTableRowElement, ReviewRowProps>(function ReviewRow(
  { item, selected, onSelect, onNext, onValidated, cardName },
  ref,
) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  // The entry as the description cell of the entries tab sees it, so renaming works the same here (RF-46)
  const entry: BillEntryModel = {
    id: item.entryId,
    displayDescription: item.displayDescription ?? item.originalDescription,
    originalDescription: item.originalDescription,
    _links: item.aliasId != null || (item.displayDescription && item.displayDescription !== item.originalDescription)
      ? { 'restore-original': { href: `/api/bill-entries/${item.entryId}/alias` } }
      : {},
  } as BillEntryModel;
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
        <span className="font-medium">
          <DescriptionCell entry={entry} onChanged={() => void queryClient.invalidateQueries()} />
        </span>
        <span className="block text-xs text-(--color-text-muted)">
          {[item.bankCategory, [cardName, item.cardLastDigits && `•••• ${item.cardLastDigits}`].filter(Boolean).join(' '), item.billingPeriod]
            .filter(Boolean)
            .join(' · ')}
        </span>
        {item.unknownFinal && <span className="block text-xs text-amber-600">{t('import.unknownFinal')}</span>}
        {item.refundOfPurchaseId && <span className="block text-xs text-emerald-600">{t('review.refund')}</span>}
        <input
          className="input mt-1 w-full max-w-64 py-0.5 text-xs"
          aria-label={t('review.note')}
          placeholder={t('review.note')}
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
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
      <td className="px-2 py-2 [&_select]:max-w-36">
        <SplitEditor idPrefix={`review-${item.purchaseId}`} value={splits} onChange={setSplits} memberPicker={MemberPickerWithAdd} />
      </td>
      <td className="px-2 py-2 [&_select]:max-w-36">
        <CategoryPickerWithAdd aria-label={t('review.category')} allowEmpty value={categoryId} onChange={setCategoryId} />
      </td>
      {/* Kept in view at the right edge even when the table scrolls sideways */}
      <td className="sticky right-0 bg-(--color-surface) px-2 py-2 shadow-[-6px_0_6px_-6px_rgba(0,0,0,0.25)]">
        <button type="button" className="button button-primary" onClick={() => void validate()}>
          {t('review.validate')}
        </button>
      </td>
    </tr>
  );
});

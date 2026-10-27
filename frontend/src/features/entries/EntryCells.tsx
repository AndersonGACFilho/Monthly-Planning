import { Check, RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { editEntry, renameEntry } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import type { BillEntryModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { useAction } from '../../shared/hooks/useAction';

/** Props shared by the inline editors. */
interface CellProps {
  /** Entry. */
  entry: BillEntryModel;
  /** Called after a change, to refresh the list. */
  onChanged: () => void;
}

/**
 * Description cell: shows the alias, renames inline for every entry with the description or only this one,
 * and restores the original (RF-46 to RF-50, UI-05).
 *
 * @param props - entry and refresh callback
 * @returns the cell content
 */
export function DescriptionCell({ entry, onChanged }: CellProps) {
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(entry.displayDescription ?? '');
  const [onlyThis, setOnlyThis] = useState(false);
  const restore = useAction(entry, 'restore-original');

  if (!editing) {
    return (
      <span className="inline-flex items-center gap-1">
        <button type="button" className="text-left hover:underline" title={entry.originalDescription} onClick={() => setEditing(true)}>
          {entry.displayDescription}
        </button>
        {restore.available && (
          <button
            type="button"
            className="icon-button"
            aria-label={t('entries.restoreOriginal')}
            title={`${t('entries.restoreOriginal')}: ${entry.originalDescription}`}
            onClick={() => restore.mutate({ method: 'DELETE' }, { onSuccess: onChanged })}
          >
            <RotateCcw size={12} aria-hidden />
          </button>
        )}
      </span>
    );
  }

  const save = async () => {
    try {
      await renameEntry(entry.id!, { displayName: name, scope: onlyThis ? 'ONLY_THIS' : 'ALL' });
      setEditing(false);
      onChanged();
    } catch (error) {
      reportError(error);
    }
  };

  return (
    <span className="flex flex-col gap-1">
      <span className="flex items-center gap-1">
        <input
          className="input w-56"
          aria-label={t('entries.alias')}
          autoFocus
          value={name}
          onChange={(e) => setName(e.target.value)}
          onKeyDown={(e) => {
            // The keys stay here: the review queue row validates on Enter and skips on Escape (RF-78)
            if (e.key === 'Enter' || e.key === 'Escape') e.stopPropagation();
            if (e.key === 'Enter') void save();
            if (e.key === 'Escape') setEditing(false);
          }}
        />
        <button type="button" className="icon-button" aria-label={t('action.save')} onClick={() => void save()}>
          <Check size={14} aria-hidden />
        </button>
      </span>
      <label className="flex items-center gap-1 text-xs">
        <input type="checkbox" checked={onlyThis} onChange={(e) => setOnlyThis(e.target.checked)} />
        {t('entries.onlyThis')}
      </label>
      <span className="text-xs text-(--color-text-muted)">{entry.originalDescription}</span>
    </span>
  );
}

/**
 * Category cell with inline editing; applies to every installment of the purchase (RF-43, RN-08).
 *
 * @param props - entry and refresh callback
 * @returns the cell content
 */
export function CategoryCell({ entry, onChanged }: CellProps) {
  const { t } = useTranslation();
  return (
    <CategoryPicker
      aria-label={t('review.category')}
      allowEmpty
      value={entry.categoryId ?? null}
      onChange={async (categoryId) => {
        try {
          await editEntry(entry.id!, { categoryId: categoryId ?? undefined, clearCategory: categoryId == null }, ifMatch(entry.version));
          onChanged();
        } catch (error) {
          reportError(error);
        }
      }}
    />
  );
}

/**
 * Member cell with inline editing; shows "split" when the purchase is divided (RF-43, RF-26).
 *
 * @param props - entry and refresh callback
 * @returns the cell content
 */
export function MemberCell({ entry, onChanged }: CellProps) {
  const { t } = useTranslation();
  const splits = entry.splits ?? [];
  if (splits.length > 1) {
    return <span className="text-xs">{t('entries.splitBetween', { count: splits.length })}</span>;
  }
  return (
    <MemberPicker
      aria-label={t('review.member')}
      value={splits[0]?.memberId ?? null}
      onChange={async (memberId) => {
        if (memberId == null) return;
        try {
          await editEntry(entry.id!, { splits: [{ memberId, percentage: 100 }], clearCategory: false }, ifMatch(entry.version));
          onChanged();
        } catch (error) {
          reportError(error);
        }
      }}
    />
  );
}

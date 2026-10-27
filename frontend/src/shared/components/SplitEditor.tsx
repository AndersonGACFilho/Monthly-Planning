import { Plus, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { MemberPicker } from './MemberPicker';

/** Share of a member by percentage (RF-26). */
export interface SplitShare {
  memberId: number | null;
  percentage: number;
}

/** Props of {@link SplitEditor}. */
export interface SplitEditorProps {
  /** Current shares. */
  value: SplitShare[];
  /** Called with the new shares. */
  onChange: (value: SplitShare[]) => void;
  /** Prefix for input ids. */
  idPrefix: string;
}

/**
 * Edits the member of a purchase, or a split between members by percentage; shows when the total is not
 * 100% (RF-26, RF-76).
 *
 * @param props - {@link SplitEditorProps}
 * @returns the editor
 */
export function SplitEditor({ value, onChange, idPrefix }: SplitEditorProps) {
  const { t } = useTranslation();
  const total = value.reduce((sum, s) => sum + (Number.isFinite(s.percentage) ? s.percentage : 0), 0);
  const update = (index: number, patch: Partial<SplitShare>) =>
    onChange(value.map((s, i) => (i === index ? { ...s, ...patch } : s)));

  return (
    <div className="flex flex-col gap-1">
      {value.map((share, index) => (
        <div key={index} className="flex items-center gap-1">
          <MemberPicker
            id={`${idPrefix}-member-${index}`}
            aria-label={t('review.member')}
            value={share.memberId}
            onChange={(memberId) => update(index, { memberId })}
          />
          {value.length > 1 && (
            <>
              <input
                type="number"
                min={0}
                max={100}
                className="input w-20 text-right"
                aria-label={t('review.percentage')}
                value={share.percentage}
                onChange={(e) => update(index, { percentage: Number(e.target.value) })}
              />
              <span aria-hidden>%</span>
              <button type="button" className="icon-button" aria-label={t('review.removeShare')} onClick={() => onChange(value.filter((_, i) => i !== index))}>
                <X size={14} aria-hidden />
              </button>
            </>
          )}
        </div>
      ))}
      <div className="flex items-center gap-2 text-xs">
        <button
          type="button"
          className="inline-flex items-center gap-1 text-(--color-accent)"
          onClick={() => {
            const even = Math.floor(100 / (value.length + 1));
            onChange([...value.map((s) => ({ ...s, percentage: even })), { memberId: null, percentage: 100 - even * value.length }]);
          }}
        >
          <Plus size={12} aria-hidden /> {t('review.split')}
        </button>
        {value.length > 1 && total !== 100 && (
          <span role="alert" className="text-red-600">
            {t('review.splitTotal', { total })}
          </span>
        )}
      </div>
    </div>
  );
}

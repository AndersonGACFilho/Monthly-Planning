import type { ComponentType } from 'react';
import { Plus, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { formatMoney, formatPercent } from '../format';
import { addShare, convertSplit, remainderOf, type Split, type SplitMode, type SplitShare } from '../split';
import { MemberPicker, type MemberPickerProps } from './MemberPicker';
import { MoneyInput } from './MoneyInput';

/** Props of {@link SplitEditor}. */
export interface SplitEditorProps {
  /** Current split. */
  value: Split;
  /** Called with the new split. */
  onChange: (value: Split) => void;
  /** Installment amount being split. */
  total: number;
  /** Prefix for input ids. */
  idPrefix: string;
  /** Member picker to use, such as one that can also register a member; {@link MemberPicker} by default. */
  memberPicker?: ComponentType<MemberPickerProps>;
}

/**
 * Edits the member of a purchase, or a split between members by percentage or by amount; shows what is
 * missing or over when the shares do not cover the whole (RF-26, RF-76).
 *
 * @param props - {@link SplitEditorProps}
 * @returns the editor
 */
export function SplitEditor({ value, onChange, total, idPrefix, memberPicker: Picker = MemberPicker }: SplitEditorProps) {
  const { t } = useTranslation();
  const split = value.shares.length > 1;
  const update = (index: number, patch: Partial<SplitShare>) =>
    onChange({ ...value, shares: value.shares.map((s, i) => (i === index ? { ...s, ...patch } : s)) });

  return (
    <div className="flex flex-col gap-1">
      {split && <ModeToggle mode={value.mode} onChange={(mode) => onChange(convertSplit(value, mode, total))} />}
      {value.shares.map((share, index) => (
        <div key={index} className="flex items-center gap-1">
          <Picker
            id={`${idPrefix}-member-${index}`}
            aria-label={t('review.member')}
            value={share.memberId}
            onChange={(memberId) => update(index, { memberId })}
          />
          {split && (
            <>
              {value.mode === 'percentage' ? (
                <>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    step="0.01"
                    className="input w-20 text-right"
                    aria-label={t('review.percentage')}
                    value={share.value}
                    onChange={(e) => update(index, { value: Number(e.target.value) })}
                  />
                  <span aria-hidden>%</span>
                </>
              ) : (
                <span className="w-28">
                  <MoneyInput aria-label={t('review.shareAmount')} value={share.value} onChange={(amount) => update(index, { value: amount ?? 0 })} />
                </span>
              )}
              <button
                type="button"
                className="icon-button"
                aria-label={t('review.removeShare')}
                onClick={() => onChange({ ...value, shares: value.shares.filter((_, i) => i !== index) })}
              >
                <X size={14} aria-hidden />
              </button>
            </>
          )}
        </div>
      ))}
      <div className="flex items-center gap-2 text-xs">
        <button type="button" className="inline-flex items-center gap-1 text-(--color-accent)" onClick={() => onChange(addShare(value, total))}>
          <Plus size={12} aria-hidden /> {t('review.split')}
        </button>
        {split && <RemainderWarning split={value} total={total} />}
      </div>
    </div>
  );
}

/**
 * Chooses whether the shares are percentages or amounts.
 *
 * @param props - current mode and what to do on a change
 * @returns the toggle
 */
function ModeToggle({ mode, onChange }: { mode: SplitMode; onChange: (mode: SplitMode) => void }) {
  const { t } = useTranslation();
  const option = (value: SplitMode, symbol: string, label: string) => (
    <button
      type="button"
      className={`px-2 py-0.5 ${mode === value ? 'bg-(--color-accent) text-white' : ''}`}
      aria-pressed={mode === value}
      aria-label={label}
      title={label}
      onClick={() => onChange(value)}
    >
      {symbol}
    </button>
  );
  return (
    <div role="group" aria-label={t('review.splitBy')} className="inline-flex self-start overflow-hidden rounded border border-(--color-border) text-xs">
      {option('percentage', '%', t('review.byPercentage'))}
      {option('amount', t('review.currencySymbol'), t('review.byAmount'))}
    </div>
  );
}

/**
 * Says how much of the whole is missing or over, or nothing when the split is complete.
 *
 * @param props - the split and the installment amount
 * @returns the warning, or nothing
 */
function RemainderWarning({ split, total }: { split: Split; total: number }) {
  const { t } = useTranslation();
  const remainder = remainderOf(split, total);
  if (remainder === 0) return null;
  const amount = split.mode === 'percentage' ? formatPercent(Math.abs(remainder), undefined, 2) : formatMoney(Math.abs(remainder));
  return (
    <span role="alert" className="text-red-600">
      {t(remainder > 0 ? 'review.splitMissing' : 'review.splitOver', { amount })}
    </span>
  );
}

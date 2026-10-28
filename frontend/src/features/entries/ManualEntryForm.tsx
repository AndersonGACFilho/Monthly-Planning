import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createEntry, replaceEntryPurchase } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import type { BillEntryModel, CardModel, ManualEntryRequest } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { reportError } from '../../app/queryClient';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyInput } from '../../shared/components/MoneyInput';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';
import { draftOf, type ManualEntryDraft, requestOf } from './manualEntry';

/** Props of {@link ManualEntryForm}. */
export interface ManualEntryFormProps {
  /** Period on screen, the default bill of a new entry. */
  period: string;
  /** Manual entry whose purchase is replaced, or `undefined` to create one. */
  entry?: BillEntryModel;
  /** Called with the first entry saved, or with nothing when cancelled. */
  onDone: (saved?: BillEntryModel) => void;
}

/**
 * Form for a card purchase that was not imported, with the same installment rules and the bill it goes to
 * (RF-44, RF-45); with an entry it replaces that manual purchase.
 *
 * @param props - {@link ManualEntryFormProps}
 * @returns the form
 */
export function ManualEntryForm({ period, entry, onDone }: ManualEntryFormProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const { data, isSuccess } = useCards({ size: 200 });
  const cards = embedded<CardModel>(data, 'cards').filter((c) => c.active);
  const [draft, setDraft] = useState(() => draftOf(period, entry));
  const set = <K extends keyof ManualEntryDraft>(key: K) => (value: ManualEntryDraft[K]) => setDraft((d) => ({ ...d, [key]: value }));
  const request = requestOf({ ...draft, cardId: draft.cardId ?? cards[0]?.id ?? null });
  const save = (body: ManualEntryRequest) =>
    entry?.id !== undefined ? replaceEntryPurchase(entry.id, body, ifMatch(entry.version)) : createEntry(body);

  return (
    <form
      className="panel grid grid-cols-2 gap-3 wide:grid-cols-4"
      aria-label={t(entry ? 'entries.editManual' : 'entries.manual')}
      onSubmit={async (event) => {
        event.preventDefault();
        if (!request) return;
        try {
          const result = await save(request);
          await saved();
          onDone(result);
        } catch (error) {
          reportError(error);
        }
      }}
    >
      {isSuccess && cards.length === 0 && (
        <p role="alert" className="col-span-full text-sm text-red-600">
          {t('entries.noCards')}
        </p>
      )}
      <Field label={t('import.card')} htmlFor="manual-card">
        <select id="manual-card" className="input" value={request?.cardId ?? draft.cardId ?? ''} onChange={(e) => set('cardId')(Number(e.target.value))}>
          {cards.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nickname}
            </option>
          ))}
        </select>
      </Field>
      <Field label={t('entries.purchaseDate')} htmlFor="manual-date">
        <input id="manual-date" type="date" required className="input" value={draft.purchaseDate} onChange={(e) => set('purchaseDate')(e.target.value)} />
      </Field>
      <Field label={t('entries.billingPeriod')} htmlFor="manual-period">
        <input id="manual-period" type="month" required className="input" value={draft.billingPeriod} onChange={(e) => set('billingPeriod')(e.target.value)} />
      </Field>
      <Field label={t('entry.description')} htmlFor="manual-description">
        <input id="manual-description" required maxLength={120} className="input" value={draft.description} onChange={(e) => set('description')(e.target.value)} />
      </Field>
      <Field label={t('entries.installmentAmount')} htmlFor="manual-amount">
        <MoneyInput id="manual-amount" allowNegative value={draft.amount} onChange={set('amount')} />
      </Field>
      <Field label={t('entries.installmentCount')} htmlFor="manual-count">
        <input id="manual-count" type="number" min={1} max={72} className="input" value={draft.installmentCount || ''} onChange={(e) => set('installmentCount')(Number(e.target.value))} />
      </Field>
      <Field label={t('review.member')} htmlFor="manual-member">
        <MemberPicker id="manual-member" allowEmpty value={draft.memberId} onChange={set('memberId')} />
      </Field>
      <Field label={t('review.category')} htmlFor="manual-category">
        <CategoryPicker id="manual-category" allowEmpty value={draft.categoryId} onChange={set('categoryId')} />
      </Field>
      <div className="col-span-full flex justify-end gap-2">
        <button type="button" className="button" onClick={() => onDone()}>
          {t('action.cancel')}
        </button>
        <button type="submit" className="button button-primary" disabled={!request || !draft.description}>
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

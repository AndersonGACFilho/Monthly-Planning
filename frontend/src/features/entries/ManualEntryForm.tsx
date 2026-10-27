import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { createEntry } from '../../api/generated/endpoints/bill-entry-controller/bill-entry-controller';
import { useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import type { CardModel } from '../../api/generated/model';
import { reportError } from '../../app/queryClient';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyInput } from '../../shared/components/MoneyInput';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';

/**
 * Form for a card purchase that was not imported, with the same installment rules (RF-44, RF-45).
 *
 * @param props - called when saved or cancelled
 * @returns the form
 */
export function ManualEntryForm({ onDone }: { onDone: () => void }) {
  const { t } = useTranslation();
  const saved = useSaved();
  const { data } = useCards({ size: 200 });
  const cards = embedded<CardModel>(data, 'cards').filter((c) => c.active);
  const [cardId, setCardId] = useState<number | null>(null);
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState<number | null>(null);
  const [installments, setInstallments] = useState(1);
  const [memberId, setMemberId] = useState<number | null>(null);
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const card = cardId ?? cards[0]?.id ?? null;

  return (
    <form
      className="panel grid grid-cols-2 gap-3 wide:grid-cols-4"
      aria-label={t('entries.manual')}
      onSubmit={async (event) => {
        event.preventDefault();
        if (card == null || amount == null) return;
        try {
          await createEntry({
            cardId: card,
            purchaseDate: date,
            description,
            amount,
            installmentCount: installments,
            memberId: memberId ?? undefined,
            categoryId: categoryId ?? undefined,
          });
          await saved();
          onDone();
        } catch (error) {
          reportError(error);
        }
      }}
    >
      <Field label={t('import.card')} htmlFor="manual-card">
        <select id="manual-card" className="input" value={card ?? ''} onChange={(e) => setCardId(Number(e.target.value))}>
          {cards.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nickname}
            </option>
          ))}
        </select>
      </Field>
      <Field label={t('entries.purchaseDate')} htmlFor="manual-date">
        <input id="manual-date" type="date" required className="input" value={date} onChange={(e) => setDate(e.target.value)} />
      </Field>
      <Field label={t('entry.description')} htmlFor="manual-description">
        <input id="manual-description" required maxLength={120} className="input" value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>
      <Field label={t('entries.installmentAmount')} htmlFor="manual-amount">
        <MoneyInput id="manual-amount" allowNegative value={amount} onChange={setAmount} />
      </Field>
      <Field label={t('entries.installmentCount')} htmlFor="manual-count">
        <input id="manual-count" type="number" min={1} max={72} className="input" value={installments} onChange={(e) => setInstallments(Number(e.target.value))} />
      </Field>
      <Field label={t('review.member')} htmlFor="manual-member">
        <MemberPicker id="manual-member" allowEmpty value={memberId} onChange={setMemberId} />
      </Field>
      <Field label={t('review.category')} htmlFor="manual-category">
        <CategoryPicker id="manual-category" allowEmpty value={categoryId} onChange={setCategoryId} />
      </Field>
      <div className="col-span-full flex justify-end gap-2">
        <button type="button" className="button" onClick={onDone}>
          {t('action.cancel')}
        </button>
        <button type="submit" className="button button-primary" disabled={amount == null || !description}>
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { changeCardCycle } from '../../api/generated/endpoints/card-controller/card-controller';
import type { CardModel } from '../../api/generated/model';
import { Field } from '../../shared/components/Field';
import { useSaved } from '../../shared/hooks/useSaved';
import { useBillingPeriod } from '../../shared/state/billingPeriod';

/** Props of {@link CycleForm}. */
export interface CycleFormProps {
  /** Card whose days change. */
  card: CardModel;
  /** Called after saving or cancelling. */
  onDone: () => void;
}

/**
 * Changes closing and due day from a billing period on, without recalculating past bills (RF-04).
 *
 * @param props - {@link CycleFormProps}
 * @returns the form
 */
export function CycleForm({ card, onDone }: CycleFormProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const current = card.cycles?.[card.cycles.length - 1];
  const period = useBillingPeriod();
  const [closingDay, setClosingDay] = useState(current?.closingDay ?? 1);
  const [dueDay, setDueDay] = useState(current?.dueDay ?? 10);
  const [validFrom, setValidFrom] = useState(period);

  return (
    <form
      className="panel flex flex-wrap items-end gap-3"
      aria-label={t('card.changeCycle')}
      onSubmit={async (event) => {
        event.preventDefault();
        await changeCardCycle(card.id!, { closingDay, dueDay, validFrom });
        await saved();
        onDone();
      }}
    >
      <Field label={t('card.closingDay')} htmlFor="cycle-closing">
        <input id="cycle-closing" type="number" min={1} max={31} className="input w-24" value={closingDay} onChange={(e) => setClosingDay(Number(e.target.value))} />
      </Field>
      <Field label={t('card.dueDay')} htmlFor="cycle-due">
        <input id="cycle-due" type="number" min={1} max={31} className="input w-24" value={dueDay} onChange={(e) => setDueDay(Number(e.target.value))} />
      </Field>
      <Field label={t('card.validFrom')} htmlFor="cycle-from">
        <input id="cycle-from" type="month" className="input" value={validFrom} onChange={(e) => setValidFrom(e.target.value)} />
      </Field>
      <button type="button" className="button" onClick={onDone}>
        {t('action.cancel')}
      </button>
      <button type="submit" className="button button-primary">
        {t('action.save')}
      </button>
    </form>
  );
}

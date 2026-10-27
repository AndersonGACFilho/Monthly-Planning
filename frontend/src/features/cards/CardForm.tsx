import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { createCard, replaceCard, useCards } from '../../api/generated/endpoints/card-controller/card-controller';
import type { CardModel } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { embedded } from '../../shared/hal';
import { useSaved } from '../../shared/hooks/useSaved';

const schema = z.object({
  nickname: z.string().trim().min(1).max(60),
  finals: z.string().regex(/^\s*\d{4}(\s*,\s*\d{4})*\s*$/),
  holderName: z.string().trim().min(1).max(80),
  primaryCardId: z.number().nullable(),
  defaultMemberId: z.number({ message: 'required' }),
  closingDay: z.number().int().min(1).max(31),
  dueDay: z.number().int().min(1).max(31),
});

type CardFormValues = z.infer<typeof schema>;

/** Props of {@link CardEditor}. */
export interface CardEditorProps {
  /** Card being edited, or `undefined` to create one. */
  card?: CardModel;
  /** Called after saving or cancelling. */
  onDone: () => void;
}

/**
 * Form to create or edit a card: nickname, finals, holder, primary card and default member; closing and
 * due day on creation (RF-02, RF-03, RF-05).
 *
 * @param props - {@link CardEditorProps}
 * @returns the form
 */
export function CardEditor({ card, onDone }: CardEditorProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const { data } = useCards({ size: 200 });
  const primaries = embedded<CardModel>(data, 'cards').filter((c) => c.primaryCardId == null && c.id !== card?.id);
  const firstCycle = card?.cycles?.[card.cycles.length - 1];
  const { register, handleSubmit, control, formState } = useForm<CardFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      nickname: card?.nickname ?? '',
      finals: card?.lastDigits?.join(', ') ?? '',
      holderName: card?.holderName ?? '',
      primaryCardId: card?.primaryCardId ?? null,
      defaultMemberId: card?.defaultMemberId,
      closingDay: firstCycle?.closingDay ?? 1,
      dueDay: firstCycle?.dueDay ?? 10,
    },
  });
  const errors = formState.errors;

  const submit = handleSubmit(async (values) => {
    const request = {
      nickname: values.nickname,
      bank: 'ITAU' as const,
      lastDigits: values.finals.split(',').map((f) => f.trim()),
      holderName: values.holderName,
      primaryCardId: values.primaryCardId ?? undefined,
      defaultMemberId: values.defaultMemberId,
      closingDay: values.closingDay,
      dueDay: values.dueDay,
    };
    if (card?.id !== undefined) await replaceCard(card.id, request, ifMatch(card.version));
    else await createCard(request);
    await saved();
    onDone();
  });

  return (
    <form onSubmit={submit} className="panel grid grid-cols-2 gap-3 wide:grid-cols-4" aria-label={t('card.form')}>
      <Field label={t('card.nickname')} htmlFor="card-nickname" error={errors.nickname && t('validation.required')}>
        <input id="card-nickname" className="input" {...register('nickname')} />
      </Field>
      <Field label={t('card.finals')} htmlFor="card-finals" error={errors.finals && t('card.finalsHint')}>
        <input id="card-finals" className="input" placeholder="4102, 4090" {...register('finals')} />
      </Field>
      <Field label={t('card.holderName')} htmlFor="card-holder" error={errors.holderName && t('validation.required')}>
        <input id="card-holder" className="input uppercase" {...register('holderName')} />
      </Field>
      <Field label={t('card.defaultMember')} htmlFor="card-member" error={errors.defaultMemberId && t('validation.required')}>
        <Controller
          control={control}
          name="defaultMemberId"
          render={({ field }) => <MemberPicker id="card-member" value={field.value} onChange={(v) => field.onChange(v ?? undefined)} />}
        />
      </Field>
      <Field label={t('card.primaryCard')} htmlFor="card-primary">
        <Controller
          control={control}
          name="primaryCardId"
          render={({ field }) => (
            <select
              id="card-primary"
              className="input"
              value={field.value ?? ''}
              onChange={(event) => field.onChange(event.target.value ? Number(event.target.value) : null)}
            >
              <option value="">{t('card.noPrimary')}</option>
              {primaries.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nickname}
                </option>
              ))}
            </select>
          )}
        />
      </Field>
      {!card && (
        <>
          <Field label={t('card.closingDay')} htmlFor="card-closing" error={errors.closingDay && t('validation.day')}>
            <input id="card-closing" type="number" min={1} max={31} className="input" {...register('closingDay', { valueAsNumber: true })} />
          </Field>
          <Field label={t('card.dueDay')} htmlFor="card-due" error={errors.dueDay && t('validation.day')}>
            <input id="card-due" type="number" min={1} max={31} className="input" {...register('dueDay', { valueAsNumber: true })} />
          </Field>
        </>
      )}
      <div className="col-span-full flex justify-end gap-2">
        <button type="button" className="button" onClick={onDone}>
          {t('action.cancel')}
        </button>
        <button type="submit" className="button button-primary" disabled={formState.isSubmitting}>
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}

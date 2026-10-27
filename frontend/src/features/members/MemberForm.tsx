import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import type { z } from 'zod';
import { createMember, replaceMember } from '../../api/generated/endpoints/member-controller/member-controller';
import type { MemberModel } from '../../api/generated/model';
import { CreateMemberBody } from '../../api/generated/zod/member-controller/member-controller.zod';
import { ifMatch } from '../../api/requestOptions';
import { Field } from '../../shared/components/Field';
import { useSaved } from '../../shared/hooks/useSaved';

type MemberForm = z.infer<typeof CreateMemberBody>;

const emptyToUndefined = (value: string) => (value === '' ? undefined : value);

/** Props of {@link MemberEditor}. */
export interface MemberEditorProps {
  /** Member being edited, or `undefined` to create one. */
  member?: MemberModel;
  /** Called after saving or cancelling. */
  onDone: () => void;
}

/**
 * Form to create or edit a member: code, name, kind, colour, contacts, PIX key and statement language
 * (RF-01, RF-80, XLS-04).
 *
 * @param props - {@link MemberEditorProps}
 * @returns the form
 */
export function MemberEditor({ member, onDone }: MemberEditorProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const { register, handleSubmit, control, formState } = useForm<MemberForm>({
    resolver: zodResolver(CreateMemberBody),
    defaultValues: {
      code: member?.code ?? '',
      name: member?.name ?? '',
      kind: member?.kind ?? 'MAIN',
      color: member?.color ?? '#2563EB',
      email: member?.email ?? undefined,
      whatsappPhone: member?.whatsappPhone ?? undefined,
      pixKey: member?.pixKey ?? undefined,
      preferredLanguage: member?.preferredLanguage ?? 'pt-BR',
    },
  });
  const errors = formState.errors;

  const submit = handleSubmit(async (values) => {
    const data = {
      ...values,
      email: values.email || undefined,
      whatsappPhone: values.whatsappPhone || undefined,
      pixKey: values.pixKey || undefined,
    };
    if (member?.id !== undefined) await replaceMember(member.id, data, ifMatch(member.version));
    else await createMember(data);
    await saved();
    onDone();
  });

  return (
    <form onSubmit={submit} className="panel grid grid-cols-2 gap-3 wide:grid-cols-4" aria-label={t('member.form')}>
      <Field label={t('member.code')} htmlFor="member-code" error={errors.code && t('member.codeHint')}>
        <input id="member-code" className="input uppercase" maxLength={4} {...register('code', { setValueAs: (v: string) => v.toUpperCase() })} />
      </Field>
      <Field label={t('member.name')} htmlFor="member-name" error={errors.name && t('validation.required')}>
        <input id="member-name" className="input" {...register('name')} />
      </Field>
      <Field label={t('member.kind')} htmlFor="member-kind">
        <select id="member-kind" className="input" {...register('kind')}>
          <option value="MAIN">{t('member.kinds.MAIN')}</option>
          <option value="THIRD_PARTY">{t('member.kinds.THIRD_PARTY')}</option>
        </select>
      </Field>
      <Field label={t('member.color')} htmlFor="member-color" error={errors.color && t('validation.invalid')}>
        <Controller
          control={control}
          name="color"
          render={({ field }) => (
            <input id="member-color" type="color" className="input h-9 w-16 p-1" value={field.value} onChange={field.onChange} />
          )}
        />
      </Field>
      <Field label={t('member.email')} htmlFor="member-email" error={errors.email && t('validation.invalid')}>
        <input id="member-email" type="email" className="input" {...register('email', { setValueAs: emptyToUndefined })} />
      </Field>
      <Field label={t('member.whatsappPhone')} htmlFor="member-phone" error={errors.whatsappPhone && t('member.phoneHint')}>
        <input id="member-phone" className="input" placeholder="+5551999990000" {...register('whatsappPhone', { setValueAs: emptyToUndefined })} />
      </Field>
      <Field label={t('member.pixKey')} htmlFor="member-pix" error={errors.pixKey && t('validation.invalid')}>
        <input id="member-pix" className="input" {...register('pixKey', { setValueAs: emptyToUndefined })} />
      </Field>
      <Field label={t('member.preferredLanguage')} htmlFor="member-language">
        <select id="member-language" className="input" {...register('preferredLanguage')}>
          <option value="pt-BR">{t('language.pt-BR')}</option>
          <option value="en-US">{t('language.en-US')}</option>
        </select>
      </Field>
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
